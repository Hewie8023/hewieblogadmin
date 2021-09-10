package com.hewie.blog.pojo;

import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table ( name ="tb_article" )
public class ArticleNoContent {

  	@Id
	private String id;
  	@Column(name = "title" )
	private String title;
  	@Column(name = "user_id" )
	private String userId;
  	@Column(name = "category_id" )
	private String categoryId;
  	//0:富文本，1：markdown
  	@Column(name = "type" )
	private String type;
  	@Column(name = "cover" )
	private String cover;
  	//0:删除，1：发表，2：草稿，3：置顶
  	@Column(name = "state" )
	private String state = "1";
  	@Column(name = "summary" )
	private String summary;
  	@Column(name = "labels" )
	private String labels;
  	@Column(name = "view_count" )
	private long viewCount = 0L;
  	@Column(name = "create_time" )
	private Date createTime;
  	@Column(name = "update_time" )
	private Date updateTime;

	@OneToOne(targetEntity = HewieUserNoPassword.class)
	@JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
	private HewieUserNoPassword hewieUserNoPassword;

	@OneToOne(targetEntity = Category.class)
	@JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false)
	@NotFound(action= NotFoundAction.IGNORE)
	private Category category;
	@Transient
	private List<String> labelList = new ArrayList<>();

	public List<String> getLabelList() {
		return labelList;
	}

	public void setLabelList(List<String> labelList) {
		this.labelList = labelList;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}


	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}


	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}


	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}


	public String getLabels() {
		//label切割
		this.labelList.clear();
		if (this.labels != null) {
			if (!this.labels.contains("-")) {
				this.labelList.add((this.labels));
			} else {
				String[] split = this.labels.split("-");
				List<String> strings = Arrays.asList(split);
				this.labelList.addAll(strings);
			}
		}
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}


	public long getViewCount() {
		return viewCount;
	}

	public void setViewCount(long viewCount) {
		this.viewCount = viewCount;
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

	public HewieUserNoPassword getHewieUserNoPassword() {
		return hewieUserNoPassword;
	}

	public void setHewieUserNoPassword(HewieUserNoPassword hewieUserNoPassword) {
		this.hewieUserNoPassword = hewieUserNoPassword;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}
}
