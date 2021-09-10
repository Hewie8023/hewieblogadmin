package com.hewie.blog.service.impl;


import com.hewie.blog.dao.ArticleDao;
import com.hewie.blog.pojo.Article;
import com.hewie.blog.utils.Constants;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class SolrTestService {

    @Autowired
    private SolrClient solrClient;

    public void add() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", "870463554774368256");
        doc.setField("blog_view_count", 10);
        doc.setField("blog_title", "数学");
        doc.setField("blog_content", "学数学");
        doc.setField("blog_create_time", new Date());
        doc.setField("blog_labels", "数学-java");
        doc.setField("blog_url", "www.baidu.com");
        doc.setField("blog_category_id", "869500160265158656");

        try {
            solrClient.add(doc);
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void update() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", "870463554774368256");
        doc.setField("blog_view_count", 10);
        doc.setField("blog_title", "数学111");
        doc.setField("blog_content", "学数学111");
        doc.setField("blog_create_time", new Date());
        doc.setField("blog_labels", "数学-java");
        doc.setField("blog_url", "www.baidu.com");
        doc.setField("blog_category_id", "869500160265158656");

        try {
            solrClient.add(doc);
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void delete() {
        try {
            solrClient.deleteById("870463554774368256");
            solrClient.deleteByQuery("*");
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    private ArticleDao articleDao;

    public void importAll() {
        List<Article> all = articleDao.findAll();
        for (Article article : all) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("id", article.getId());
            doc.setField("blog_view_count", article.getViewCount());
            doc.setField("blog_title", article.getTitle());
            //对内容进行处理，去掉标签，提取纯文本
            //1、markdown，type=1,==>html==>纯文本
            //2、富文本，type=0，==》纯文本
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
            doc.setField("blog_url", "www.baidu.com");
            doc.setField("blog_category_id", article.getCategoryId());

            try {
                solrClient.add(doc);
                solrClient.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteAll() {
        try {
            solrClient.deleteByQuery("*");
            solrClient.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
