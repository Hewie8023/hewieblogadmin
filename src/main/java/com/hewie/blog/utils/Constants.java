package com.hewie.blog.utils;

public interface Constants {

    String FROM_PC = "p_";
    String FROM_MOBILE = "m_";

    String  APP_DOWNLOAD_PATH = "/portal/app";

    interface User {
        String ROLE_ADMIN = "role_admin";
        String ROLE_NORMAL = "role_normal";
        String DEFAULT_AVATAR = "https://cdn.sunofbeaches.com/images/default_avatar.png";
        String DEFAULT_STATE = "1";
        String KEY_CAPTCHA_CONTENT = "key_captcha_content_";
        String KEY_EMAIL_CONTENT = "key_email_content_";
        String KEY_EMAIL_SEND_IP = "key_email_send_ip_";
        String KEY_EMAIL_SEND_ADDRESS = "key_email_send_address_";
        String KEY_TOKEN = "key_token_";
        String KEY_COOKIE_TOKEN = "hewie_blog_token";
        String KEY_COMMIT_TOKEN_RECORD = "key_commit_token_record_";
        String KEY_PC_LOGIN_ID = "key_pc_login_id_";
        String KEY_PC_LOGIN_STATE_FALSE = "false";
        int QR_CODE_STATE_CHECK_WAITING_TIME = 30;
        String KEY_LAST_REQUEST_LOGIN_ID=  "k_l_r_l_i";
        String KEY_LAST_CAPTCHA_ID=  "k_l_c_i";
    }

    interface Settings {
        String MANAGER_ACCOUNT_INIT = "manager_account_init";
        String WEBSITE_TITLE = "website_title";
        String WEBSITE_DESCRIPTION = "website_description";
        String WEBSITE_KEYWORDS = "website_keywords";
        String WEBSITE_VIEW_COUNT = "website_view_count";
    }

    interface TimeValueInMillions {
        // 单位ms
        long MIN = 60 * 1000;
        long HOUR = 60 * MIN;
        long DAY = 24 * HOUR;
        long WEEK = 7 * DAY;
        long MONTH = 30 * DAY;

        long HOUR_2 = HOUR * 2;
    }

    interface TimeValueInSecond {
        // 单位s
        int FIVE_SEC = 5;
        int TEN_SEC = 10;
        int MIN = 60;
        int HOUR = 60 * MIN;
        int DAY = 24 * HOUR;
        int WEEK = 7 * DAY;
        int MONTH = 30 * DAY;

        long HOUR_2 = 60 * 60 * 2;
    }

    interface Page {
        int DEFAULT_PAGE = 1;
        int MIN_SIZE = 5;
    }

    interface ImageType {
        String PREFIX = "image/";
        String TYPE_JPG = "jpg";
        String TYPE_PNG = "png";
        String TYPE_GIF = "gif";
        String TYPE_JPEG = "jpeg";
        String TYPE_JPG_WITH_PREFIX = PREFIX + "jpg";
        String TYPE_PNG_WITH_PREFIX = PREFIX + "png";
        String TYPE_GIF_WITH_PREFIX = PREFIX + "gif";
        String TYPE_JPEG_WITH_PREFIX = PREFIX + "jpeg";
    }


    interface Article {
        int TITLE_MAX_LEN = 128;
        int SUMMARY_MAX_LEN = 256;
        String STATE_DELETE = "0";
        String STATE_PUBLISH = "1";
        String STATE_DRAFT = "2";
        String STATE_TOP = "3";
        String TYPE_MARKDOWN = "1";
        String TYPE_RICH_TEXT = "0";
        String KEY_ARTICLE_CACHE = "key_article_cache_";
        String KEY_ARTICLE_VIEW_COUNT = "key_article_view_count_";
        String KEY_ARTICLE_LIST_FIRST_PAGE = "key_article_list_first_page";
        String OWN = "own";
        String OTHER = "other";
        String KEY_TOP_TEN_ARTICLE = "key_top_ten_article";
        String KEY_LAST_TEN_ARTICLE = "key_last_ten_article";
    }

    interface Comment {
        String STATE_PUBLISH = "1";
        String STATE_TOP = "2";
        String KEY_COMMENT_FIRST_PAGE = "key_comment_first_page_";
    }
}
