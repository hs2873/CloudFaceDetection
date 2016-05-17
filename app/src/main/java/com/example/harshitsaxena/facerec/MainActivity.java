package com.example.harshitsaxena.facerec;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.InputStreamEntity;

public class MainActivity extends AppCompatActivity {

    private int TAKE_PHOTO=0;
    boolean livePicture=false;
    ImageView imageView;
    String picturePath;
    Bitmap bitmap=null;
   // java.net.URI fileUri;

    android.net.Uri fileUri;
    TextView tv;
    AsyncHttpClient client;
    Canvas canvas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        tv=(TextView)findViewById(R.id.textView);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //Start the camera intent and take a picture in real time
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "FaceRec.jpg");
                fileUri= Uri.fromFile(file);
                livePicture=true;
                Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
                startActivityForResult(intent, 100);

            }
        });
        imageView=(ImageView)findViewById(R.id.imageView);


    }


    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK) {

            if (!livePicture) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);
                bitmap = BitmapFactory.decodeFile(picturePath);
            } else {
                try {
                    picturePath = fileUri.getPath();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                imageView.setImageBitmap(bitmap);
            }


            //File file = new File(picturePath);

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            canvas = new Canvas(mutableBitmap);

            final Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);


            imageView.setImageBitmap(mutableBitmap);
            //canvas.drawRect(20, 20, 100, 100, paint);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(picturePath, options);
            int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            client = new AsyncHttpClient();
            //tv.setText(byteArray.length);
            Log.e("test", imageHeight + "width" + imageWidth);

            //tv.setText(file.toString());
            String url = "http://54.172.125.1/";
            RequestParams params = new RequestParams();

            params.put("image", new ByteArrayInputStream(byteArray), "image.jpeg", "text/plain");
            final ByteArrayInputStream istream = new ByteArrayInputStream(byteArray);

            final AsyncHttpResponseHandler asyncHttpResponseHandler = new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    //String base64String =.printBase64Binary(responseBody);
                    String s = new String(responseBody);
                    //Decode the response and find out the coordinates of the rectangles
                    try {
                        JSONObject jsonObj = new JSONObject(s);
                        String s1 = (String) jsonObj.get("body");
                        String delim = " \n\r\t "; //insert here all delimitators
                        StringTokenizer st = new StringTokenizer(s1, delim);
                        String[] res = new String[st.countTokens()];
                        int j = 0;
                        while (st.hasMoreTokens()) {
                            res[j] = st.nextToken();
                            j++;
                        }

                        tv.setText(s1);

                        for (int i = 0; i < Integer.parseInt(res[0]); i++) {
                            if (Double.parseDouble(res[5 * (i + 1)]) > -.4)
                                canvas.drawRect(Integer.parseInt(res[5 * i + 1]), Integer.parseInt(res[5 * i + 2]), Integer.parseInt(res[5 * i + 1]) + Integer.parseInt(res[5 * i + 3]), Integer.parseInt(res[5 * i + 2]) + Integer.parseInt(res[5 * i + 4]), paint);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e("test", Arrays.toString(headers));
                }
            };
            InputStreamEntity myEntity = new InputStreamEntity(istream, byteArray.length);

            client.post(this, url, myEntity, "text/plain", asyncHttpResponseHandler);

        }

    }


        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            livePicture=false;
            startActivityForResult(i, 1);

        }

        return super.onOptionsItemSelected(item);
    }
}
