package com.hewie.blog.pojo;

import java.util.ArrayList;
import java.util.List;

public class CommentRes {

	private Firstcomment firstcomment;
  	private List<Replay> replayList = new ArrayList<>();

	public Firstcomment getFirstcomment() {
		return firstcomment;
	}

	public void setFirstcomment(Firstcomment firstcomment) {
		this.firstcomment = firstcomment;
	}

	public List<Replay> getReplayList() {
		return replayList;
	}

	public void setReplayList(List<Replay> replayList) {
		this.replayList = replayList;
	}
}
