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

import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

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

public class AsyncRemoteCall extends AsyncTask<Integer, Void, Void> {

    public String stringParameter;
    public ArrayList<Parser> results = new ArrayList<>();
    public int [] tasks;
    private Listener listener;
    public static final int ACTION_LOOKUP_TV = 1;
    public static final int ACTION_LOOKUP_MOVIE = 2;
    private static final String TAG = "lfe";
    private static final String CLASS = "AsyncBackendCall";


    public interface Listener {
        default void onPostExecute(AsyncRemoteCall taskRunner) {}
    }

    public AsyncRemoteCall(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Integer... tasks) {
        this.tasks = new int[tasks.length];
        for (int count = 0; count < tasks.length; count++) {
            int task = tasks[count];
            this.tasks[count] = task;
            switch (task) {
                case ACTION_LOOKUP_TV:
                case ACTION_LOOKUP_MOVIE:
                    Parser parser = new TmdbParser(task, stringParameter);
                    String urlString = parser.getUrlString();
                    fetch(urlString,null,parser);
                    results.add(parser);
                    break;
            }
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

    public interface Parser {
        public void parseStream(InputStream in);
        public String getUrlString();
    }

    public static class TmdbParser implements Parser {
        public int task;
        public String parameter;
        public ArrayList<TmdbEntry> entries = new ArrayList<>();
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
                                        TmdbEntry entry = new TmdbEntry();
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

    public static class TmdbEntry {
        public int id;
        public String name;
        public String firstAirDate;
        public String overview;
    }

}
