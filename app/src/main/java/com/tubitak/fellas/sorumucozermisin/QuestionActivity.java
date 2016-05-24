package com.tubitak.fellas.sorumucozermisin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tubitak.fellas.sorumucozermisin.classes.Answer;
import com.tubitak.fellas.sorumucozermisin.classes.AnswerAdapter;
import com.tubitak.fellas.sorumucozermisin.classes.Question;
import com.tubitak.fellas.sorumucozermisin.classes.QuestionAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QuestionActivity extends AppCompatActivity {
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    private RecyclerView recyclerView;
    private AnswerAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    ArrayList<Answer> answers = new ArrayList<>();
    Question myQ;
    private TextView title;
    private View photo;
    private TextView question;
    private TextView username;
    private TextView date;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        myQ = new Question(
                getIntent().getIntExtra("idQuestion",-1),
                getIntent().getStringExtra("username"),
                getIntent().getStringExtra("title"),
                getIntent().getStringExtra("question"),
                getIntent().getStringExtra("photo"),
                getIntent().getStringExtra("date")
        );
        myQ.setBitmapPhoto((Bitmap) getIntent().getParcelableExtra("bitmap"));
        title = (TextView) findViewById(R.id.titleQuestion);
        photo = (ImageButton) findViewById(R.id.photoQuestion);
        question = (TextView) findViewById(R.id.question);
        username = (TextView) findViewById(R.id.usernameQuestion);
        date = (TextView) findViewById(R.id.dateQuestion);
        title.setText(myQ.getTitle());
        question.setText(myQ.getQuestion());
        username.setText(myQ.getUsername() + " tarafindan soruldu");
        date.setText(myQ.getDate());
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), myQ.getBitmapPhoto());
        photo.setBackground(bitmapDrawable);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomImageFromThumb(photo);
            }
        });
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_answer);
        mAdapter = new AnswerAdapter(answers);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        initialize();
    }

    private void initialize() {
        new answerListAsync().execute();
    }

    public class answerListAsync extends AsyncTask<Void,Void,String>
    {
        @Override
        protected String doInBackground(Void... params) {
            String result = null;
            String url = "http://188.166.167.178/getAnswers.php";
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("idquestion", String.valueOf(myQ.getId()))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Call call = client.newCall(request);
            Response response = null;
            try
            {
                response = call.execute();
                if (response.isSuccessful())
                {
                    result = response.body().string();
                }
                else
                {
                    result = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String answers) {
            super.onPostExecute(answers);
            List<Answer> answerList= new Vector<Answer>();
            JSONArray reader = null;
            if(answers!=null) {
                try {
                    reader = new JSONArray(answers);
                    for(int i=0;i<reader.length();i++)
                    {
                        JSONObject r = reader.getJSONObject(i);
                        answerList.add(new Answer(r.getInt("idanswer"),
                                r.getString("username"),
                                r.getString("answer"),
                                r.getString("dateanswer")
                        ));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            mAdapter.updateList(answerList);
        }
    }

    private void zoomImageFromThumb(final View thumbView) {
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        final ImageView expandedImageView = (ImageView) findViewById(
                R.id.expanded_image);
        expandedImageView.setImageBitmap(myQ.getBitmapPhoto());

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }
}