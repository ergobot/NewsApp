package com.example.android.newsapp;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends ListActivity {

    private ArrayList<Article> articles = null;
    private ArticleAdapter articleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articles = new ArrayList<Article>();
        this.articleAdapter = new ArticleAdapter(this, R.layout.row, articles);
        setListAdapter(this.articleAdapter);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener(){


            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Article article = (Article)adapterView.getItemAtPosition(i);
                if(article.getContentUrl() != null && !article.getContentUrl().isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                    browserIntent.setData(Uri.parse(article.getContentUrl()));
                    startActivity(browserIntent);
                }
            }


        });

        queryArticles(null);

    }

    public void queryArticles(View view){
        // asynctask
        ArticleQueryTask articleQueryTask = new ArticleQueryTask();
        articleQueryTask.execute();
    }


    private class ArticleAdapter extends ArrayAdapter<Article> {

        private ArrayList<Article> items;

        public ArticleAdapter(Context context, int textViewResourceId, ArrayList<Article> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ArticleHolder holder = new ArticleHolder();
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.row, null);
            }
            Article article = items.get(position);
            if (article != null) {
                TextView title = (TextView) view.findViewById(R.id.title);
                ImageView tile = (ImageView) view.findViewById(R.id.storyimage);
                holder.articleTitleView = title;
                holder.img = tile;
                view.setTag(holder);

                Article a = articles.get(position);
                holder.articleTitleView.setText(a.getTitle());

                if (title != null) {
                    title.setText(article.getTitle());
                }

                if(article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {

                    if(tile != null) {
                        new DownloadImageTask(holder).execute(article.getImageUrl());
                    }
                }

            }
            return view;
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ArticleHolder holder) {
            this.bmImage = holder.img;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if(result != null) {
                bmImage.setImageBitmap(result);
            }
        }
    }

    private static class ArticleHolder {
        public TextView articleTitleView;
        public ImageView img;
    }

    public class ArticleQueryTask extends AsyncTask<String, Void, ArrayList<Article>> {

        private final String LOG_TAG = ArticleQueryTask.class.getSimpleName();

        private ArrayList<Article> getBookDataFromJson(String bookJsonStr)
                throws JSONException {

            JSONObject bookJson = new JSONObject(bookJsonStr);
            ArrayList<Article> articlesResults = new ArrayList<Article>();
            if(bookJson.has("response") && bookJson.getJSONObject("response").has("results")){
                JSONArray resultsArray = bookJson.getJSONObject("response").getJSONArray("results");
                    for(int i = 0; i < resultsArray.length(); i++) {
                        JSONObject element = (JSONObject)resultsArray.get(i);
                        String title = "Unknown Title";
                        String contentUrl = "";
                        String imageUrl = "";
                        if (element.has("webTitle")) {
                            title = element.getString("webTitle");
                        }
                        if (element.has("webUrl")) {
                            contentUrl = element.getString("webUrl");
                        }
                        if (element.has("fields")) {
                            imageUrl = element.getJSONObject("fields").getString("thumbnail");
                        }

                        Article book = new Article();
                        book.setTitle(title);
                        book.setContentUrl(contentUrl);
                        book.setImageUrl(imageUrl);
                        articlesResults.add(book);
                    }


            }



            return articlesResults;

        }
        @Override
        protected ArrayList<Article> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
//            if (params.length == 0) {
//                return null;
//            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String bookJsonStr = null;

            try {
                final String BOOK_QUERY_BASE_URL =
                        "http://content.guardianapis.com/search?show-fields=thumbnail&q=android&api-key=test&page-size=10";

                Uri builtUri = Uri.parse(BOOK_QUERY_BASE_URL).buildUpon().build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to queryapi, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {


                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    bookJsonStr = buffer.toString();

                    Log.v(LOG_TAG, "Book result string: " + bookJsonStr);
                }else{
                    // Returns null when response code is not good response code
                    return null;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getBookDataFromJson(bookJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the book results.
            return null;
        }


        @Override
        public void onPostExecute(ArrayList<Article> result){
            if(result != null){
                articleAdapter.clear();
                articleAdapter.addAll(result);
                articleAdapter.notifyDataSetChanged();
            }

        }

    }





}
