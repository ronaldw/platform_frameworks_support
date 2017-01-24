/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.sample.moviebrowser.network;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import com.android.sample.moviebrowser.ContributorData;
import com.android.sample.moviebrowser.PersonData;
import com.android.sample.moviebrowser.RepositoryData;
import com.android.sample.moviebrowser.model.AuthTokenModel;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This class is responsible for loading data from network.
 */
public class GithubNetworkManager {
    private static GithubNetworkManager sInstance;

    private GithubService mGithubService;
    private AuthTokenModel mAuthTokenModel;

    /**
     * Gets the singleton instance of this manager.
     */
    public static synchronized GithubNetworkManager getInstance() {
        if (sInstance == null) {
            sInstance = new GithubNetworkManager();
        }

        return sInstance;
    }

    /**
     * Interface that exposes successful / failed calls to the rest of the application.
     *
     * @param <T> Payload data class.
     */
    public interface NetworkCallListener<T> {
        /** Called when network response returned empty data, passing back the HTTP code. */
        void onLoadEmpty(int httpCode);

        /** Called when data has been succesfully loaded from the network. */
        void onLoadSuccess(T data);

        /** Called when data has failed loading from the network. */
        void onLoadFailure();
    }

    /**
     * Interface that exposes the option to cancel an existing network call.
     */
    public interface Cancelable {
        /** Cancel the ongoing network call. */
        void cancel();
    }

    private class CancelableCall implements Cancelable {
        @NonNull private Call mCall;

        private CancelableCall(@NonNull Call call) {
            mCall = call;
        }

        @Override
        public void cancel() {
            mCall.cancel();
        }
    }

    private GithubNetworkManager() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                HttpUrl originalHttpUrl = original.url();

                HttpUrl url = originalHttpUrl.newBuilder()
                        .addQueryParameter("access_token",
                                mAuthTokenModel.getAuthTokenData().getValue())
                        .build();

                Request.Builder requestBuilder = original.newBuilder().url(url);

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();

        mGithubService = retrofit.create(GithubService.class);
    }

    /**
     * Sets authentication token model to be used on all future requests.
     */
    public void setAuthTokenModel(AuthTokenModel authTokenModel) {
        mAuthTokenModel = authTokenModel;
    }

    /**
     * Fetches the specified page of repositories.
     */
    @MainThread
    public CancelableCall listRepositories(String user, int pageIndex,
            final NetworkCallListener<List<RepositoryData>> networkCallListener) {
        Call<List<RepositoryData>> listRepositoriesCall = mGithubService.listRepositories(
                user, pageIndex);
        listRepositoriesCall.enqueue(new Callback<List<RepositoryData>>() {
            @Override
            public void onResponse(Call<List<RepositoryData>> call,
                    Response<List<RepositoryData>> response) {
                List<RepositoryData> body = response.body();
                if (body == null) {
                    networkCallListener.onLoadEmpty(response.code());
                } else {
                    networkCallListener.onLoadSuccess(body);
                }
            }

            @Override
            public void onFailure(Call<List<RepositoryData>> call, Throwable t) {
                android.util.Log.e("GithubBrowser", "Call = " + call.toString(), t);
                networkCallListener.onLoadFailure();
            }
        });
        return new CancelableCall(listRepositoriesCall);
    }

    /**
     * Fetches the specified page of contributors.
     */
    @MainThread
    public CancelableCall getContributors(String owner, String project, int page,
            final NetworkCallListener<List<ContributorData>> networkCallListener) {
        Call<List<ContributorData>> getContributorsCall = mGithubService.getContributors(
                owner, project, page);
        getContributorsCall.enqueue(new Callback<List<ContributorData>>() {
            @Override
            public void onResponse(Call<List<ContributorData>> call,
                    Response<List<ContributorData>> response) {
                List<ContributorData> body = response.body();
                if (body == null) {
                    networkCallListener.onLoadEmpty(response.code());
                } else {
                    networkCallListener.onLoadSuccess(body);
                }
            }

            @Override
            public void onFailure(Call<List<ContributorData>> call, Throwable t) {
                android.util.Log.e("GithubBrowser", "Call = " + call.toString(), t);
                networkCallListener.onLoadFailure();
            }
        });
        return new CancelableCall(getContributorsCall);
    }

    /**
     * Fetches the details of the specified user.
     */
    @MainThread
    public CancelableCall getUser(String user,
            final NetworkCallListener<PersonData> networkCallListener) {
        Call<PersonData> getUserCall = mGithubService.getUser(user);
        getUserCall.enqueue(new Callback<PersonData>() {
            @Override
            public void onResponse(Call<PersonData> call,
                    Response<PersonData> response) {
                PersonData body = response.body();
                if (body == null) {
                    networkCallListener.onLoadEmpty(response.code());
                } else {
                    networkCallListener.onLoadSuccess(body);
                }
            }

            @Override
            public void onFailure(Call<PersonData> call, Throwable t) {
                android.util.Log.e("GithubBrowser", "Call = " + call.toString(), t);
                networkCallListener.onLoadFailure();
            }
        });
        return new CancelableCall(getUserCall);
    }
}
