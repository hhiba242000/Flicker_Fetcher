package android.bignerdranch.photogallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PhotoPageFragment extends VisibleFragment{
    private static final String ARG_URI = "photo_page_url";

    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private onBackButtonPressed mBackPress;

    public static PhotoPageFragment newInstance(Uri uri) {
        //here the hosting activity of the fragment will need to pass it the uri of the webpage to display
        //so you put the uri in a bundle, create your fragment,then pass the bundle to the fragment
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //retrieve the uri from fragment bundle
        mUri = getArguments().getParcelable(ARG_URI);
    }

    public interface onBackButtonPressed{
         void onBackButtonPressed(WebView mWebView);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBackPress=(onBackButtonPressed)context;


    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);
        mProgressBar = (ProgressBar)v.findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);
        mWebView = (WebView) v.findViewById(R.id.web_view);
        mBackPress.onBackButtonPressed(mWebView);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView webView, int newProgress) {
                //fun responsible of the progress bar to simulate the loading of a picture
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            public void onReceivedTitle(WebView webView, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(mUri.toString());
        return v;
    }
}



