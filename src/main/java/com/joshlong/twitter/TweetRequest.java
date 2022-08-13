package com.joshlong.twitter;

import java.util.Date;

record TweetRequest(String clientId, String clientSecret, String twitterUsername, String jsonRequest, Date scheduled) {
}
