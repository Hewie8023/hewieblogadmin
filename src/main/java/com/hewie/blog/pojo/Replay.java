package com.hewie.blog.pojo;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import java.util.Date;

@Entity
@Table ( name ="tb_replay" )
public class Replay {

  	@Id
	private String id;
  	@Column(name = "father_comment_id" )
	private String fatherCommentId;
  	@Column(name = "type" )
	private String type = "0";
  	@Column(name = "content" )
	private String content;
  	@Column(name = "from_uid" )
	private String fromUid;
  	@Column(name = "from_uname" )
	private String fromUname;
  	@Column(name = "from_uavatar" )
	private String fromUavatar;
  	@Column(name = "to_uid" )
	private String toUid;
  	@Column(name = "to_uname" )
	private String toUname;
  	@Column(name = "to_uavatar" )
	private String toUavatar;
  	@Column(name = "state" )
	private String state = "1";
  	@Column(name = "create_time" )
	private Date createTime;
  	@Column(name = "update_time" )
	private Date updateTime;

	@Column(name = "article_id")
	private String articleId;

	public String getArticleId() {
		return articleId;
	}

	public void setArticleId(String articleId) {
		this.articleId = articleId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public String getFatherCommentId() {
		return fatherCommentId;
	}

	public void setFatherCommentId(String fatherCommentId) {
		this.fatherCommentId = fatherCommentId;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}


	public String getFromUid() {
		return fromUid;
	}

	public void setFromUid(String fromUid) {
		this.fromUid = fromUid;
	}


	public String getFromUname() {
		return fromUname;
	}

	public void setFromUname(String fromUname) {
		this.fromUname = fromUname;
	}


	public String getFromUavatar() {
		return fromUavatar;
	}

	public void setFromUavatar(String fromUavatar) {
		this.fromUavatar = fromUavatar;
	}


	public String getToUid() {
		return toUid;
	}

	public void setToUid(String toUid) {
		this.toUid = toUid;
	}


	public String getToUname() {
		return toUname;
	}

	public void setToUname(String toUname) {
		this.toUname = toUname;
	}


	public String getToUavatar() {
		return toUavatar;
	}

	public void setToUavatar(String toUavatar) {
		this.toUavatar = toUavatar;
	}


	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}


	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}


	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

}
