/*
 * Copyright (c) Joaquim Ley 2016. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.joaquimley.core.data;

import android.support.annotation.StringDef;
import android.util.Log;

import com.joaquimley.core.BuildConfig;
import com.joaquimley.core.data.model.CharacterMarvel;
import com.joaquimley.core.data.model.Comic;
import com.joaquimley.core.data.model.DataWrapper;
import com.joaquimley.core.data.network.MarvelService;
import com.joaquimley.core.data.network.MarvelServiceFactory;
import com.joaquimley.core.ui.base.RemoteCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import retrofit2.Call;

/**
 * Api abstraction
 */
public class DataManager {

    private static DataManager sInstance;

    private final MarvelService mMarvelService;

    public static DataManager getInstance() {
        if (sInstance == null) {
            sInstance = new DataManager();
        }
        return sInstance;
    }

    private DataManager() {
        mMarvelService = MarvelServiceFactory.makeMarvelService();
    }

    public void getCharactersList(int offSet, int limit, String searchQuery,
                                  RemoteCallback<DataWrapper<List<CharacterMarvel>>> listener) {
        long timeStamp = System.currentTimeMillis();
        mMarvelService.getCharacters(BuildConfig.PUBLIC_KEY,
                buildMd5AuthParameter(timeStamp), timeStamp, offSet, limit, searchQuery)
                .enqueue(listener);
    }

    public void getCharacter(long characterId, RemoteCallback<DataWrapper<List<CharacterMarvel>>> listener) {
        long timeStamp = System.currentTimeMillis();
        mMarvelService.getCharacter(characterId, BuildConfig.PUBLIC_KEY,
                buildMd5AuthParameter(timeStamp), timeStamp)
                .enqueue(listener);
    }


    private static final String COMIC_TYPE_COMICS = "comics";
    private static final String COMIC_TYPE_SERIES = "series";
    private static final String COMIC_TYPE_STORIES = "stories";
    private static final String COMIC_TYPE_EVENTS = "events";


    @StringDef({COMIC_TYPE_COMICS, COMIC_TYPE_SERIES, COMIC_TYPE_STORIES, COMIC_TYPE_EVENTS})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Type {
    }

    public void getComics(long characterId, Integer offset, Integer limit, RemoteCallback<DataWrapper<List<Comic>>> listener) {
        getComicListByType(characterId, COMIC_TYPE_COMICS, offset, limit).enqueue(listener);
    }

    public void getSeries(long characterId, Integer offset, Integer limit, RemoteCallback<DataWrapper<List<Comic>>> listener) {
        getComicListByType(characterId, COMIC_TYPE_SERIES, offset, limit).enqueue(listener);
    }

    public void getStories(long characterId, Integer offset, Integer limit, RemoteCallback<DataWrapper<List<Comic>>> listener) {
        getComicListByType(characterId, COMIC_TYPE_STORIES, offset, limit).enqueue(listener);
    }

    public void getEvents(long characterId, Integer offset, Integer limit, RemoteCallback<DataWrapper<List<Comic>>> listener) {
        getComicListByType(characterId, COMIC_TYPE_EVENTS, offset, limit).enqueue(listener);
    }

    /**
     * Base request to prevent code duplication
     *
     * @param id        {@link CharacterMarvel} Id
     * @param comicType Which {@link .Type} list should be requested
     */
    private Call<DataWrapper<List<Comic>>> getComicListByType(long id, @Type String comicType,
                                                              Integer offset, Integer limit) {
        long timeStamp = System.currentTimeMillis();
        return mMarvelService.getCharacterComics(id, comicType, offset, limit, BuildConfig.PUBLIC_KEY,
                buildMd5AuthParameter(timeStamp), timeStamp);
    }

    /**
     * Builds the required API "hash" parameter (timeStamp + privateKey + publicKey)
     *
     * @param timeStamp Current timeStamp
     * @return MD5 hash string
     */
    private static String buildMd5AuthParameter(long timeStamp) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest((timeStamp + BuildConfig.PRIVATE_KEY
                    + BuildConfig.PUBLIC_KEY).getBytes());
            BigInteger number = new BigInteger(1, messageDigest);

            String md5 = number.toString(16);
            while (md5.length() < 32) {
                md5 = 0 + md5;
            }
            return md5;

        } catch (NoSuchAlgorithmException e) {
            Log.e("DataManager", "Error hashing required parameters: " + e.getMessage());
            return "";
        }
    }
}

/**
 * -- Request response list --
 * 409	Limit greater than 100.
 * 409	Limit invalid or below 1.
 * 409	Invalid or unrecognized parameter.
 * 409	Empty parameter.
 * 409	Invalid or unrecognized ordering parameter.
 * 409	Too many values sent to a multi-value list filter.
 * 409	Invalid value passed to filter.
 * <p/>
 * <p/>
 * -- Authorization --
 * 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 * 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 * 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 * 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 * 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 * 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 * 403	Forbidden
 * <p/>
 * -- Authorization --
 * 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 * 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 * 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 * 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 * 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 * 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 * 403	Forbidden
 * <p/>
 * -- Authorization --
 * 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 * 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 * 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 * 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 * 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 * 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 * 403	Forbidden
 * <p/>
 * -- Authorization --
 * 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 * 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 * 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 * 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 * 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 * 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 * 403	Forbidden
 * <p>
 * -- Authorization --
 * 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 * 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 * 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 * 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 * 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 * 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 * 403	Forbidden
 */

/**
 -- Authorization --
 409	Missing API Key	Occurs when the apikey parameter is not included with a request.
 409	Missing Hash	Occurs when an apikey parameter is included with a request, a ts parameter is present, but no hash parameter is sent. Occurs on server-side applications only.
 409	Missing Timestamp	Occurs when an apikey parameter is included with a request, a hash parameter is present, but no ts parameter is sent. Occurs on server-side applications only.
 401	Invalid Referer	Occurs when a referrer which is not valid for the passed apikey parameter is sent.
 401	Invalid Hash	Occurs when a ts, hash and apikey parameter are sent but the hash is not valid per the above hash generation rule.
 405	Method Not Allowed	Occurs when an API endpoint is accessed using an HTTP verb which is not allowed for that endpoint.
 403	Forbidden

 */