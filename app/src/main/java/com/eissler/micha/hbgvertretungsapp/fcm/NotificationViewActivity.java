package com.eissler.micha.hbgvertretungsapp.fcm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.koushikdutta.ion.Ion;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationViewActivity extends AppCompatActivity {

    @BindView(R.id.image_view)
    ImageView imageView;

    @BindView(R.id.text_view_title)
    TextView tv_title;

    @BindView(R.id.text_view_body)
    TextView tv_body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);
        ButterKnife.bind(this);
        System.out.println("NotificationViewActivity.onCreate");

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("NotificationViewActivity.onResume");
    }

    private void checkIntent(Intent intent) {
        System.out.println("NotificationViewActivity.checkIntent");
        Bundle data = intent.getExtras();

        final String title = data.getString("title");
        final String body = data.getString("body");
        final String imageUrl = data.getString("imageUrl");

        tv_title.setText(title);
        tv_body.setText(body);

        System.out.println("imageUrl = " + imageUrl);
        if (imageUrl == null || imageUrl.equals("")) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            Ion.with(NotificationViewActivity.this)
                    .load(imageUrl)
                    .withBitmap()
                    .placeholder(R.drawable.ic_image_black_48dp)
                    .error(R.drawable.ic_error_black_48dp)
                    .intoImageView(imageView);
        }
    }
}
