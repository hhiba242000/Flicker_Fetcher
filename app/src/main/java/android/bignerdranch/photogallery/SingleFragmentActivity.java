package android.bignerdranch.photogallery;

import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

abstract class SingleFragmentActivity extends AppCompatActivity {
    //an interface i made up that extends a fragment,like it should
    //an abstract method to be implemented
    protected abstract Fragment createFragment();

    //to choose which layout file,is it for phone or tablet?
    @LayoutRes
    protected int getLayoutResId(){
        return R.layout.activity_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        //when creating the fragment it's either already existing and retrieved by id,or
        // will be created and added to the backstack
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}