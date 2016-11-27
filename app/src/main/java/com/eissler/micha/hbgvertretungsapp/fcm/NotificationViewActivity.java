package com.eissler.micha.hbgvertretungsapp.fcm;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.eissler.micha.hbgvertretungsapp.R;
import com.koushikdutta.ion.Ion;

import it.gmariotti.cardslib.library.cards.material.MaterialLargeImageCard;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardViewNative;

public class NotificationViewActivity extends AppCompatActivity {

    CardViewNative cardViewNative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        Bundle data = getIntent().getExtras();

        final String title = data.getString("title");
        final String body = data.getString("body");
        final String imageUrl = data.getString("imageUrl");

        System.out.println("imageUrl = " + imageUrl);
        if (imageUrl != null && !imageUrl.equals("")) {
            cardViewNative = (CardViewNative) findViewById(R.id.notification_image_card);
            cardViewNative.setVisibility(View.VISIBLE);

            Toast.makeText(this, R.string.act_nv_download_image, Toast.LENGTH_SHORT).show();
            MaterialLargeImageCard.SetupWizard cardBuilder = MaterialLargeImageCard.with(this)
                    .setTitle(title)
                    .setSubTitle(body)
                    .useDrawableUrl(imageUrl)
                    .useDrawableExternal(new MaterialLargeImageCard.DrawableExternal() {
                        @Override
                        public void setupInnerViewElements(ViewGroup parent, View viewImage) {
                            Animation animation = AnimationUtils.loadAnimation(NotificationViewActivity.this, R.anim.anim_refresh);
                            animation.setRepeatCount(Animation.INFINITE);
                            Ion.with(NotificationViewActivity.this)
                                    .load(imageUrl)
                                    .withBitmap()
                                    .placeholder(R.drawable.ic_refresh_black_48dp)
                                    .error(R.drawable.ic_error_black_24dp)
                                    .animateLoad(animation)
                                    .intoImageView((ImageView) viewImage);
                        }
                    });
            cardViewNative.setCard(cardBuilder.build());
        } else {
            cardViewNative = (CardViewNative) findViewById(R.id.notification_card);
            cardViewNative.setVisibility(View.VISIBLE);
            Card card = new Card(this);
            CardHeader header = new CardHeader(this);
            header.setTitle(title);
            card.addCardHeader(header);
            card.setTitle(body);
            cardViewNative.setCard(card);
        }

    }

//    @Override // TODO: 01.11.2016 provide proper up-navigation
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            // Respond to the action bar's Up/Home button
//            case android.R.id.home:
//                Intent upIntent = NavUtils.getParentActivityIntent(this);
//                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
//                    // This activity is NOT part of this app's task, so create a new task
//                    // when navigating up, with a synthesized back stack.
//                    TaskStackBuilder.create(this)
//                            // Add all of this activity's parents to the back stack
//                            .addNextIntentWithParentStack(upIntent)
//                            // Navigate up to the closest parent
//                            .startActivities();
//                } else {
//                    // This activity is part of this app's task, so simply
//                    // navigate up to the logical parent activity.
//                    NavUtils.navigateUpTo(this, upIntent);
//                }
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
