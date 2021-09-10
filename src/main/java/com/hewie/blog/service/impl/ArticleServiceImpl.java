package com.hewie.blog.service.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.hewie.blog.dao.ArticleDao;
import com.hewie.blog.dao.ArticleNoContentDao;
import com.hewie.blog.dao.CommentDao;
import com.hewie.blog.dao.LabelDao;
import com.hewie.blog.pojo.*;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IArticleService;
import com.hewie.blog.service.ISolrService;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.RedisUtil;
import com.hewie.blog.utils.SnowflakeIdWorker;
import com.hewie.blog.utils.TextUtils;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.*;

@Slf4j
@Service
@Transactional
public class ArticleServiceImpl extends BaseService implements IArticleService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private ArticleDao articleDao;

    @Autowired
    private IUserService userService;

    @Autowired
    private ArticleNoContentDao articleNoContentDao;

    @Autowired
    private Random random;

    @Autowired
    private ISolrService solrService;

    @Autowired
    private RedisUtil redisUtil;
    /**
     * 发表文章
     *
     * todo:定时发布的功能
     * 多人：考虑审核-->成功，通知
     *
     * 保存草稿：
     * 1、用户手动提交：发生页面跳转--提交完即可
     * 2、代码自动提交，每隔一段时间就提交，不会发生页面跳转，导致多次提交，如果没有唯一标识，就会重复提交
     *
     * 必须有标题
     * 方案一：每次用户发新文章之前，先向后台请求一个唯一的文章ID
     * 如果是更新文件，则不需要请求这个唯一的ID
     * 这时，提交文章就携带了ID
     *
     * 方案二：直接提交，后台判断有没有ID，如果没有ID，就新创建，并且ID作为此次返回的结果，如果有ID，就修改已经存在的内容
     *
     * 防止重复提交：
     * 1、可以通过ID的方式
     * 2、通过token—Key的提交频率来计算，如果30s之内有多次提交，只有最前的一次有效,其他的提交，直接return，提示用户不要太频繁
     *
     * 前端的处理：点击了提交之后，禁止按钮可以使用，等到有响应结果再改变按钮状态。
     *
     * @param article
     * @return
     */
    @Override
    public ResponseResult postArticle(Article article) {

        //检查用户，获取用户对象
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        //检查数据
        String title = article.getTitle();
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("文章标题不能为空");
        }
        if (title.length() > Constants.Article.TITLE_MAX_LEN) {
            return ResponseResult.FAILED("文章标题不能超过" + Constants.Article.TITLE_MAX_LEN + "个字符");
        }

        String state = article.getState();
        if (!Constants.Article.STATE_PUBLISH.equals(state) && !Constants.Article.STATE_DRAFT.equals(state)) {
            return ResponseResult.FAILED("不支持此操作");
        }
        String type = article.getType();
        if (TextUtils.isEmpty(type)) {
            return ResponseResult.FAILED("类型不能为空");
        }
        if (!"0".equals(type) && !"1".equals(type)) {
            return ResponseResult.FAILED("类型不正确");
        }

        // 以下是发布的检查，草稿不需要检查
        if (Constants.Article.STATE_PUBLISH.equals(state)) {
            String categoryId = article.getCategoryId();
            if (TextUtils.isEmpty(categoryId)) {
                return ResponseResult.FAILED("文章类别不能为空");
            }
            String content = article.getContent();
            if (TextUtils.isEmpty(content)) {
                return ResponseResult.FAILED("文章内容不能为空");
            }
            String summary = article.getSummary();
            if (TextUtils.isEmpty(summary)) {
                return ResponseResult.FAILED("文章摘要不能为空");
            }
            if (summary.length() > Constants.Article.SUMMARY_MAX_LEN) {
                return ResponseResult.FAILED("文章摘要不能超过" + Constants.Article.SUMMARY_MAX_LEN + "个字符");
            }
            String labels = article.getLabels();
            //标签1-标签2-标签3
            if (TextUtils.isEmpty(labels)) {
                return ResponseResult.FAILED("文章标签不能为空");
            }
        }
        String articleId = article.getId();
        if (TextUtils.isEmpty(articleId)) {
            //新的内容
            //补充数据
            article.setId(idWorker.nextId() + "");
            article.setCreateTime(new Date());
        } else {
            //对状态进行处理，如果是已经发布的，则不能改为草稿
            Article articleFromDb = articleDao.findOneById(articleId);
            if (Constants.Article.STATE_PUBLISH.equals(articleFromDb.getState()) &&
                Constants.Article.STATE_DRAFT.equals(state)) {
                return ResponseResult.FAILED("已经发布的文章不支持保存草稿");
            }
        }
        article.setUpdateTime(new Date());
        article.setUserId(hewieUser.getId());

        //保存到数据库
        articleDao.save(article);

        //保存到搜索的数据库
        if (Constants.Article.STATE_PUBLISH.equals(state)) {
            solrService.addArticle(article);
        }

        redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);

        this.setupLabels(article.getLabels());
        //返回结果，只有一种case使用到这个ID
        //如果要做程序自动保存草稿（比如每30s保存一次，就需要加上这个ID，否则会重复提交）
        return ResponseResult.SUCCESS(Constants.Article.STATE_DRAFT.equals(state) ? "草稿保存成功" : "文章发表成功").setData(article.getId());
    }

    @Autowired
    private LabelDao labelDao;

    private void setupLabels(String labels) {
        List<String>  labelList = new ArrayList<>();
        if (labels.contains("-")) {
            labelList.addAll(Arrays.asList(labels.split("-")));
        } else {
            labelList.add(labels);
        }
        //入库，统计
        for (String label : labelList) {
            //效率低
//            Label labelFromDb = labelDao.findOneByName(label);
//            if (labelFromDb == null) {
//                labelFromDb = new Label();
//                labelFromDb.setId(idWorker.nextId() + "");
//                labelFromDb.setName(label);
//                labelFromDb.setCount(0);
//                labelFromDb.setCreateTime(new Date());
//            }
//            labelFromDb.setCount(labelFromDb.getCount() + 1);
//            labelFromDb.setUpdateTime(new Date());
            int result = labelDao.updateCountByName(label);
            if (result == 0) {
                Label targetLabel = new Label();
                targetLabel.setId(idWorker.nextId() + "");
                targetLabel.setName(label);
                targetLabel.setCount(1);
                targetLabel.setCreateTime(new Date());
                targetLabel.setUpdateTime(new Date());
                labelDao.save(targetLabel);
            }
        }
    }

    /**
     * 管理中心获取文章列表
     * @param page
     * @param size
     * @param keyword
     * @param categoryId
     * @param state 0：删除、1：发布、2：草稿、3：top
     * @return
     */
    @Override
    public ResponseResult listArticles(int page, int size, String keyword, String categoryId, String state) {
        page = checkPage(page);
        size = checkSize(size);
        boolean isSearch = false;
        if (page == 1) {
            String articleListJson = (String) redisUtil.get(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            isSearch = !TextUtils.isEmpty(keyword) || !TextUtils.isEmpty(categoryId) || !TextUtils.isEmpty(state);
            if (!TextUtils.isEmpty(articleListJson) && !isSearch) {
                PageList<ArticleNoContent> result = gson.fromJson(articleListJson, new TypeToken<PageList<ArticleNoContent>>(){}.getType());
                return ResponseResult.SUCCESS("获取文章列表成功").setData(result);
            }
        }
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<ArticleNoContent> all = articleNoContentDao.findAll(new Specification<ArticleNoContent>() {
            @Override
            public Predicate toPredicate(Root<ArticleNoContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> predicateList = new ArrayList<>();
                if (!TextUtils.isEmpty(state)) {
                    Predicate statePre = cb.equal(root.get("state").as(String.class), state);
                    predicateList.add(statePre);
                }
                if (!TextUtils.isEmpty(categoryId)) {
                    Predicate categoryPre = cb.equal(root.get("categoryId").as(String.class), categoryId);
                    predicateList.add(categoryPre);
                }
                if (!TextUtils.isEmpty(keyword)) {
                    Predicate titlePre = cb.like(root.get("title").as(String.class), "%" + keyword + "%");
                    predicateList.add(titlePre);
                }
                Predicate[] preArrays = new Predicate[predicateList.size()];
                predicateList.toArray(preArrays);
                return cb.and(preArrays);
            }
        }, pageable);
        PageList<ArticleNoContent> result = new PageList<>();
        result.parsePage(all);
        if (page == 1 && !isSearch) {
            redisUtil.set(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE, gson.toJson(result), Constants.TimeValueInSecond.HOUR);
        }
        return ResponseResult.SUCCESS("获取文章列表成功").setData(result);
    }


    @Autowired
    private Gson gson;
    /**
     * 如果有审核机制，审核中的文章，只有管理员和作者自己获取
     *
     * 统计文章de阅读量
     *
     * 先把阅读量在redis里更新
     * 文章也会在redis里缓存一份，比如说10分钟
     * 当文章没的时候，从mysql中取，同时更新阅读量
     * 10分钟以后，在下一次访问的时候更新一次
     *
     *
     * @param articleId
     * @return
     */
    @Override
    public ResponseResult getArticle(String articleId,  String type) {
        //先从redis获取文章，如果没有再去mysql里获取
        String articleJson = (String) redisUtil.get(Constants.Article.KEY_ARTICLE_CACHE + articleId);
        if (type.equals("portal") && !TextUtils.isEmpty(articleJson)) {
            Article article = gson.fromJson(articleJson, Article.class);
            //增加阅读数量
            redisUtil.incr(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, 1);
            return ResponseResult.SUCCESS("文章获取成功").setData(article);
        }
        //查询出文章
        Article article = articleDao.findOneById(articleId);
        if (article == null) {
            return ResponseResult.FAILED("文章不存在");
        }


        //判断文章状态，如果是删除/草稿，需要管理员角色
        String state = article.getState();
        if (Constants.Article.STATE_PUBLISH.equals(state) || Constants.Article.STATE_TOP.equals(state)) {
            String html = null;
            //处理文章内容
            if (type.equals("portal")) {
                if (Constants.Article.TYPE_MARKDOWN.equals(article.getType())) {
                    //转成html
                    // markdown to html
                    MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
                            TablesExtension.create(),
                            JekyllTagExtension.create(),
                            TocExtension.create(),
                            SimTocExtension.create()
                    ));
                    Parser parser = Parser.builder(options).build();
                    HtmlRenderer renderer = HtmlRenderer.builder(options).build();
                    Node document = parser.parse(article.getContent());
                    html = renderer.render(document);
                } else if (Constants.Article.TYPE_RICH_TEXT.equals(article.getType())) {
                    html = article.getContent();
                }
            }
            //复制一份
            String articleStr = gson.toJson(article);
            Article newArticle = gson.fromJson(articleStr, Article.class);
            if (type.equals("portal")) {
                newArticle.setContent(html);
            }


            //正常发布的状态
            redisUtil.set(Constants.Article.KEY_ARTICLE_CACHE + articleId, gson.toJson(newArticle), Constants.TimeValueInSecond.MIN * 5);
            String viewCount = (String) redisUtil.get(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId);
            if (TextUtils.isEmpty(viewCount)) {
                long newCount = article.getViewCount() + 1;
                redisUtil.set(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, String.valueOf(newCount));
            } else {
                long newCount = redisUtil.incr(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, 1);
                //有的话就更新到mysql
                article.setViewCount(newCount);
                articleDao.save(article);
                //更新solr的阅读量
                solrService.updateArticle(articleId, article);
            }

            return ResponseResult.SUCCESS("文章获取成功").setData(newArticle);
        }
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null || !Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            return ResponseResult.PERMISSION_DENIED();
        }
        return ResponseResult.SUCCESS("文章获取成功").setData(article);
    }

    /**
     * 更新文章：标题、内容、标签、摘要、分类
     * @param articleId
     * @param article
     * @return
     */
    @Override
    public ResponseResult updateArticle(String articleId, Article article) {
        Article articleFromDb = articleDao.findOneById(articleId);
        if (articleFromDb == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        String title = article.getTitle();
        if (!TextUtils.isEmpty(title)) {
            articleFromDb.setTitle(title);
        }
        String content = article.getContent();
        if (!TextUtils.isEmpty(content)) {
            articleFromDb.setContent(content);
        }
        String labels = article.getLabels();
        if (!TextUtils.isEmpty(labels)) {
            articleFromDb.setLabels(labels);
        }
        String summary = article.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            articleFromDb.setSummary(summary);
        }
        String categoryId = article.getCategoryId();
        if (!TextUtils.isEmpty(categoryId)) {
            articleFromDb.setCategoryId(categoryId);
        }
        String cover = article.getCover();
        if (!TextUtils.isEmpty(cover)){
            articleFromDb.setCover(cover);
        }
        articleFromDb.setState(article.getState());
        redisUtil.del(Constants.Article.KEY_ARTICLE_CACHE + articleId);
        redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        articleFromDb.setUpdateTime(new Date());
        articleDao.save(articleFromDb);
        return ResponseResult.SUCCESS("文章修改成功");
    }

    @Autowired
    private CommentDao commentDao;
    @Override
    public ResponseResult deleteArticle(String articleId) {
        //先把评论删除
         commentDao.deleteAllByArticleId(articleId);
        int result = articleDao.deleteAllById(articleId);
        if (result > 0) {
            redisUtil.del(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId);
            redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            solrService.deleteArticle(articleId);
            return ResponseResult.SUCCESS("删除文章成功");

        }
        return ResponseResult.FAILED("文章不存在");
    }

    @Override
    public ResponseResult deleteArticleByState(String articleId) {
        int result = articleDao.deleteArticleByState(articleId);
        if (result > 0) {
            redisUtil.del(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId);
            redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            solrService.deleteArticle(articleId);
            return ResponseResult.SUCCESS("删除文章成功");

        }
        return ResponseResult.FAILED("文章不存在");
    }

    @Override
    public ResponseResult topArticle(String articleId) {
        //必须是已经发布的才可以置顶,如果置顶了，则取消置顶
        Article article = articleDao.findOneById(articleId);
        if (article == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        String state = article.getState();
        redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        if (Constants.Article.STATE_PUBLISH.equals(state)) {
            article.setState(Constants.Article.STATE_TOP);
            articleDao.save(article);
            return ResponseResult.SUCCESS("置顶成功");
        } else if (Constants.Article.STATE_TOP.equals(state)) {
            article.setState(Constants.Article.STATE_PUBLISH);
            articleDao.save(article);
            return ResponseResult.SUCCESS("取消置顶成功");
        }
        return ResponseResult.FAILED("不支持该操作");
    }

    /**
     * 获取置顶文章
     * 和权限无关
     * @return
     */
    @Override
    public ResponseResult listTopArticles() {
        List<ArticleNoContent> result = articleNoContentDao.findAll(new Specification<ArticleNoContent>() {
            @Override
            public Predicate toPredicate(Root<ArticleNoContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                return cb.equal(root.get("state").as(String.class), Constants.Article.STATE_TOP);
            }
        });
        return ResponseResult.SUCCESS("获取置顶文章成功").setData(result);
    }

    /**
     * 获取推荐的文章
     * @param articleId
     * @param size
     * @return
     */
    @Override
    public ResponseResult listRecommendArticles(String articleId, int size) {
        //查询文章,不需要文章内容，只需要标签
        String labels = articleDao.listArticlesById(articleId);
        //打撒标签
        List<String> labelList = new ArrayList<>();
        if (!labels.contains("-")) {
            labelList.add(labels);
        } else {
            labelList.addAll(Arrays.asList(labels.split("-")));
        }
        String targetLabel = labelList.get(random.nextInt(labelList.size()));
        log.info("targetLabel ==>" + targetLabel);
        //从列表中随机获取一个标签,查询相似的文章
        //不要内容
        List<ArticleNoContent> recommendArticles = articleNoContentDao.listArticleByLabel("%" + targetLabel + "%", articleId, size);
        //判断长度
        if (recommendArticles.size() < size) {
            int dxSize = size - recommendArticles.size();
            List<ArticleNoContent> dxList = articleNoContentDao.listLastedArticleBySize(articleId, dxSize);
            recommendArticles.addAll(dxList);
        }
        return ResponseResult.SUCCESS("获取推荐文章成功").setData(recommendArticles);
    }

    /**
     * 获取标签文章列表
     * @param page
     * @param size
     * @param label
     * @return
     */
    @Override
    public ResponseResult listArticlesByLabel(int page, int size, String label) {
        page = checkPage(page);
        size = checkSize(size);

        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<ArticleNoContent> all = articleNoContentDao.findAll(new Specification<ArticleNoContent>() {
            @Override
            public Predicate toPredicate(Root<ArticleNoContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                Predicate labelPre = cb.like(root.get("labels").as(String.class), "%" + label + "%");
                Predicate statePre = cb.equal(root.get("state").as(String.class), Constants.Article.STATE_PUBLISH);
                Predicate topPre = cb.equal(root.get("state").as(String.class), Constants.Article.STATE_TOP);
                Predicate or = cb.or(statePre, topPre);
                return cb.and(labelPre, or);
            }
        }, pageable);
        return ResponseResult.SUCCESS("获取文章列表成功").setData(all);
    }

    @Override
    public ResponseResult listLabels(int size) {
        size = this.checkSize(size);
        Sort sort = new Sort(Sort.Direction.DESC, "count");
        Pageable pageable = PageRequest.of(0, size, sort);
        Page<Label> all = labelDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取标签列表成功").setData(all);
    }

    @Override
    public ResponseResult getArticleCount() {
        long count = articleDao.count();
        return ResponseResult.SUCCESS("文章总数获取成功").setData(count);
    }

}
