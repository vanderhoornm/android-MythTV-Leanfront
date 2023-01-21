/*
 * Copyright (c) 2021 Peter Bennett
 *
 * This file is part of MythTV-leanfront.
 *
 * MythTV-leanfront is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * MythTV-leanfront is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mythtv.leanfront.data;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import androidx.annotation.Nullable;

import org.mythtv.leanfront.MyApplication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncRemoteCall implements Runnable {

    public String stringParameter;
    public ArrayList<Parser> results = new ArrayList<>();
    public Integer inTasks[];
    public int [] tasks;
    private final Listener listener;
    public static final int ACTION_LOOKUP_TVMAZE = 1;
    public static final int ACTION_LOOKUP_TV = 2;
    public static final int ACTION_LOOKUP_MOVIE = 3;
    private static final String TAG = "lfe";
    private static final String CLASS = "AsyncBackendCall";
    private final static ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Activity activity;

    public interface Listener {
        default void onPostExecute(AsyncRemoteCall taskRunner) {}
    }

    public AsyncRemoteCall(@Nullable Activity activity, @Nullable Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void execute(Integer ... tasks) {
        inTasks = tasks;
        executor.submit(this);
    }

    @Override
    public void run() {
        try {
            runTasks();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        finally {
            if (listener != null)
                if (activity == null)
                    listener.onPostExecute(this);
                else
                    activity.runOnUiThread(() -> listener.onPostExecute(this));
        }
    }

    protected Void runTasks() {
        this.tasks = new int[inTasks.length];
        for (int count = 0; count < tasks.length; count++) {
            int task = inTasks[count];
            this.tasks[count] = task;
            Parser parser;
            switch (task) {
                case ACTION_LOOKUP_TV:
                case ACTION_LOOKUP_MOVIE:
                    parser = new TmdbParser(task, stringParameter);
                    break;
                case ACTION_LOOKUP_TVMAZE:
                    parser = new TvMazeParser(task, stringParameter);
                    break;
                default:
                    return null;
            }
            String urlString = parser.getUrlString();
            fetch(urlString,null,parser);
            results.add(parser);
        }
        return null;
    }

    protected void onPostExecute(Void result) {
        if (listener != null)
            listener.onPostExecute(this);
    }


    public static int fetch(String urlString, String requestMethod, Parser parser) {
        int ret = 0;
        URL url = null;
        HttpURLConnection urlConnection = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Cache-Control", "no-cache");
            urlConnection.setConnectTimeout(5000);
            // 5 minutes - should never be this long.
            urlConnection.setReadTimeout(300000);
            if (requestMethod != null)
                urlConnection.setRequestMethod(requestMethod);
            is = urlConnection.getInputStream();
            parser.parseStream(is);
        } catch(FileNotFoundException e) {
            Log.e(TAG, CLASS + " Exception accessing: " + urlString, e);
            ret = 404;
        } catch(IOException e) {
            Log.e(TAG, CLASS + " Exception accessing: " + urlString, e);
            ret = 500;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, CLASS + " Exception closing stream: " + urlString, e);
                }
            }
        }
        return ret;
    }

    public static abstract class Parser {
        public int task;
        public String parameter;
        public ArrayList<TvEntry> entries = new ArrayList<>();
        public abstract void parseStream(InputStream in);
        public abstract String getUrlString();
    }

    public static class TmdbParser extends Parser {
        private static final String BASEURL = "https://api.themoviedb.org/3/";
        private static final String APIKEY = "c27cb71cff5bd76e1a7a009380562c62";
        // https://api.themoviedb.org/3/search/tv?query=Monk&api_key=c27cb71cff5bd76e1a7a009380562c62
        // &language=en&page=1

        public TmdbParser(int task, String parameter) {
            this.task = task;
            this.parameter = parameter;
        }

        @Override
        public String getUrlString() {
            StringBuilder urlString = new StringBuilder(BASEURL);
            switch (task) {
                case ACTION_LOOKUP_TV:
                    urlString.append("search/tv?");
                    break;
                case ACTION_LOOKUP_MOVIE:
                    urlString.append("search/movie?");
                    break;
            }

            Context context = MyApplication.getAppContext();
            Locale locale;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                locale = context.getResources().getConfiguration().getLocales().get(0);
            }
            else
                locale = context.getResources().getConfiguration().locale;
            try {
                urlString.append("query=")
                        .append(URLEncoder.encode(parameter, "UTF-8"))
                        .append("&api_key=")
                        .append(APIKEY)
                        .append("&language=")
                        .append(locale.getLanguage())
                        .append("&page=1");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, CLASS + " Exception encoding URL: " + parameter, e);
            }
            return urlString.toString();
        }

        @Override
        public void parseStream(InputStream in) {
            // Structure of json:
            // {
            //   "**OTHER TAGS**": XXXX,
            //   "results": [
            //     {
            //       "first_air_date": "2002-07-12",
            //       "id": 1695,
            //       "name": "Monk",
            //       "overview": "Adrian Monk was once a rising star with the ...",
            //       "**OTHER_TAGS**": XXXX,
            //     },
            //     {
            //       **MORE_RESULTS** ...
            //     }
            //   ],
            //   "**OTHER TAGS**": XXXX,
            // }
            JsonReader reader = null;
            try {
                reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
                reader.beginObject();
                doc:
                for (;;) {
                    switch (reader.peek()) {
                        case NAME:
                            String name = reader.nextName();
                            switch (name) {
                                case "results":
                                    reader.beginArray();
                                    results:
                                    for (;;) {
                                        if (reader.peek() == JsonToken.END_ARRAY) {
                                            reader.endArray();
                                            break results;
                                        }
                                        reader.beginObject();
                                        TvEntry entry = new TvEntry();
                                        entry:
                                        for (;;) {
                                            switch (reader.peek()) {
                                                case NAME:
                                                    String name1 = reader.nextName();
                                                    switch (name1) {
                                                        case "id":
                                                            entry.id = reader.nextInt();
                                                            break;
                                                        case "name":
                                                        case "title":
                                                            entry.name = reader.nextString();
                                                            break;
                                                        case "first_air_date":
                                                        case "release_date":
                                                            entry.firstAirDate = reader.nextString();
                                                            break;
                                                        case "overview":
                                                            entry.overview = reader.nextString();
                                                            break;
                                                        default:
                                                            reader.skipValue();
                                                    }
                                                    break;
                                                case END_OBJECT:
                                                    entries.add(entry);
                                                    reader.endObject();
                                                    break entry;
                                            }
                                        } // entry
                                    } // results
                                    break;
                                default:
                                    reader.skipValue();
                            }
                            break;
                        case END_OBJECT:
                            reader.endObject();
                            break doc;
                        default:
                    }
                } // doc
            } catch (IOException | IllegalStateException e) {
                Log.e(TAG, CLASS + " Exception parsing: " + parameter, e);
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, CLASS + " Exception closing reader: " + parameter, e);
                    }
                }
            } // finally
        } // parseStream
    } // TmdbParser

    public static class TvEntry {
        public int id;
        public String name;
        public String firstAirDate;
        public String overview;
    }

    public static class TvMazeParser extends Parser {
        private static final String BASEURL = "https://api.tvmaze.com/";
        // https://api.tvmaze.com/search/shows?q=Monk

        public TvMazeParser(int task, String parameter) {
            this.task = task;
            this.parameter = parameter;
        }

        @Override
        public String getUrlString() {
            StringBuilder urlString = new StringBuilder(BASEURL);
            urlString.append("search/shows?");

            try {
                urlString.append("q=")
                        .append(URLEncoder.encode(parameter, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, CLASS + " Exception encoding URL: " + parameter, e);
            }
            return urlString.toString();
        }

        @Override
        public void parseStream(InputStream in) {
            // Structure of json:
            // [
            //     {
            //        "**OTHER TAGS**": XXXX,
            //        "show" : {
            //           "id": 543,
            //           "name": "Mork & Mindy",
            //           "premiered": "2002-07-12",
            //           "summary": ""<p>In <b>Mork &amp; Mindy</b>, in which  ...</p>",
            //           "**OTHER_TAGS**": XXXX,
            //         },
            //     },
            //     {
            //       **MORE_RESULTS** ...
            //     }
            // ]
            JsonReader reader = null;
            try {
                reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
                reader.beginArray();
                results:
                for (;;) {
                    if (reader.peek() == JsonToken.END_ARRAY) {
                        reader.endArray();
                        break results;
                    }
                    reader.beginObject();
                    entry:
                    for (;;) {
                        switch (reader.peek()) {
                            case NAME:
                                String name1 = reader.nextName();
                                switch (name1) {
                                    case "show":
                                        TvEntry show = new TvEntry();
                                        reader.beginObject();
                                        show:
                                        for (;;) {
                                            switch (reader.peek()) {
                                                case NAME:
                                                    String name2 = reader.nextName();
                                                    switch (name2) {
                                                        case "id":
                                                            show.id = reader.nextInt();
                                                            break;
                                                        case "name":
                                                            show.name = reader.nextString();
                                                            break;
                                                        case "premiered":
                                                            show.firstAirDate = reader.nextString();
                                                            break;
                                                        case "summary":
                                                            Spanned spanned;
                                                            if (android.os.Build.VERSION.SDK_INT >= 24)
                                                                spanned = Html.fromHtml(reader.nextString(),Html.FROM_HTML_MODE_COMPACT);
                                                            else
                                                                spanned =  Html.fromHtml(reader.nextString());
                                                            show.overview = spanned.toString();
                                                            break;
                                                        default:
                                                            reader.skipValue();
                                                    }
                                                    break;
                                                case END_OBJECT:
                                                    entries.add(show);
                                                    reader.endObject();
                                                    break show;
                                            }
                                        } // show
                                        break;
                                    default:
                                        reader.skipValue();
                                }
                                break;
                            case END_OBJECT:
                                reader.endObject();
                                break entry;
                        }
                    } // entry
                } // results
            } catch (IOException | IllegalStateException e) {
                Log.e(TAG, CLASS + " Exception parsing: " + parameter, e);
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, CLASS + " Exception closing reader: " + parameter, e);
                    }
                }
            } // finally
        } // parseStream
    } // TvMazeParser


}
