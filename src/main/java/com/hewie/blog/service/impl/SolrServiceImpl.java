package com.hewie.blog.service.impl;

import com.hewie.blog.pojo.Article;
import com.hewie.blog.pojo.PageList;
import com.hewie.blog.pojo.SearchResult;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ISolrService;
import com.hewie.blog.utils.Constants;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 时机：
 * 搜索内容添加：
 *文章发表时，state=1
 * 搜索内容删除：
 *文章删除的时候，物理删除和state=0
 * 搜索内容更新：
 * TODO：当阅读量更新
 */

@Slf4j
@Service
@Transactional
public class SolrServiceImpl extends BaseService implements ISolrService {

    @Autowired
    private SolrClient solrClient;
    @Override
    public ResponseResult doSearch(String keyword, int page, int size, String categoryId, Integer sort) {
        //1、检查page，size
        page = checkPage(page);
        size = checkSize(size);
        //2、分页设置
        //每页数量
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(size);
        //设置开始的位置
        //第一页--》从0开始、第二页--》size，第三页--》2*size，第四页--》3*size，第n页--》（n-1）*size
        int start = (page - 1) * size;
        solrQuery.setStart(start);

        //3、设置搜索条件
        //关键字、条件过滤、排序、高亮
        solrQuery.set("df", "search_item");
        if (TextUtils.isEmpty(keyword)) {
            solrQuery.set("q", "*");
        } else {
            solrQuery.set("q", keyword);
        }
        //排序有四个：时间的升序（1）和降序（2），根据浏览量的升序（3）和降序（4）
        if (sort != null) {
            if (sort == 1) {
                solrQuery.setSort("blog_create_time", SolrQuery.ORDER.asc);
            } else if (sort == 2) {
                solrQuery.setSort("blog_create_time", SolrQuery.ORDER.desc);
            } else if (sort == 3) {
                solrQuery.setSort("blog_view_count", SolrQuery.ORDER.asc);
            }else if (sort == 4) {
                solrQuery.setSort("blog_view_count", SolrQuery.ORDER.desc);
            }
        }
        if (!TextUtils.isEmpty(categoryId)) {
            solrQuery.setFilterQueries("blog_category_id:" + categoryId);
        }

        solrQuery.setHighlight(true);
        solrQuery.addHighlightField("blog_title,blog_content");
        solrQuery.setHighlightSimplePre("<font color='red'>");
        solrQuery.setHighlightSimplePost("</font>");
        solrQuery.setHighlightFragsize(500);

        //设置返回字段
        solrQuery.addField("id,blog_content,blog_create_time,blog_labels,blog_url,blog_title,blog_view_count");

        //4、搜索

        try {
            QueryResponse result = solrClient.query(solrQuery);
            //5、处理搜索结果
            //获取到高亮信息
            Map<String, Map<String, List<String>>> highlighting = result.getHighlighting();
            //转成bean
            List<SearchResult> resultList = result.getBeans(SearchResult.class);
            for (SearchResult item : resultList) {
                Map<String, List<String>> stringListMap = highlighting.get(item.getId());
                if (stringListMap != null) {
                    List<String> blogContent = stringListMap.get("blog_content");
                    if (blogContent != null) {
                        item.setBlogContent(blogContent.get(0));
                    }
                    List<String> blogTitle = stringListMap.get("blog_title");
                    if (blogTitle != null) {
                        item.setBlogTitle(blogTitle.get(0));
                    }
                }
            }
            //log.info(highlighting.toString());
            //log.info(resultList.toString());
            //6、返回
            //列表、页码，每页数量
            long totalCount = result.getResults().getNumFound();
            PageList<SearchResult> pageList = new PageList<>(page, totalCount, size);
            pageList.setContents(resultList);

            return ResponseResult.SUCCESS("搜索成功").setData(pageList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseResult.FAILED("搜索失败，请稍候重试");

    }

    public void addArticle(Article article) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", article.getId());
        doc.setField("blog_view_count", article.getViewCount());
        doc.setField("blog_title", article.getTitle());
        //对内容进行处理，去掉标签，提取纯文本
        //1、markdown，type=1,==>html==>纯文本
        //2、富文本，type=0，==》纯文 本
        String type = article.getType();
        String html = null;
        if (Constants.Article.TYPE_MARKDOWN.equals(type)) {
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
        } else if (Constants.Article.TYPE_RICH_TEXT.equals(type)) {
            html = article.getContent();
        }
        // html转纯文本
        String text = Jsoup.parse(html).text();

        doc.setField("blog_content", text);
        doc.setField("blog_create_time", article.getCreateTime());
        doc.setField("blog_labels", article.getLabels());
        doc.setField("blog_url", "/article/" + article.getId());
        doc.setField("blog_category_id", article.getCategoryId());

        try {
            solrClient.add(doc);
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteArticle(String articleId) {
        try {
            solrClient.deleteById(articleId);
            solrClient.deleteByQuery("*");
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateArticle(String articleId,Article article) {
        article.setId(articleId);
        this.addArticle(article);
    }
}
