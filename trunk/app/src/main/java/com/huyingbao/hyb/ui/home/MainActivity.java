package com.huyingbao.hyb.ui.home;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hardsoftstudio.rxflux.action.RxError;
import com.hardsoftstudio.rxflux.dispatcher.RxViewDispatch;
import com.hardsoftstudio.rxflux.store.RxStore;
import com.hardsoftstudio.rxflux.store.RxStoreChange;
import com.huyingbao.hyb.R;
import com.huyingbao.hyb.HybApp;
import com.huyingbao.hyb.actions.Actions;
import com.huyingbao.hyb.actions.Keys;
import com.huyingbao.hyb.adapter.RepoAdapter;
import com.huyingbao.hyb.model.GitHubRepo;
import com.huyingbao.hyb.stores.RepositoriesStore;
import com.huyingbao.hyb.stores.UsersStore;

import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements RxViewDispatch, RepoAdapter.OnRepoClicked {

    @Bind(R.id.recycler_view)
    RecyclerView recyclerView;
    @Bind(R.id.progress_loading)
    ProgressBar progress_loading;
    @Bind(R.id.root_coordinator)
    CoordinatorLayout coordinatorLayout;
    private RepoAdapter adapter;
    private UsersStore usersStore;
    private RepositoriesStore repositoriesStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepoAdapter();
        adapter.setCallback(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.action_refresh) {
//            refresh();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRxStoreChanged(@NonNull RxStoreChange change) {
        setLoadingFrame(false);

        switch (change.getStoreId()) {
            case RepositoriesStore.STORE_ID:
                switch (change.getRxAction().getType()) {
                    case Actions.GET_PUBLIC_REPOS:
                        adapter.setRepos(repositoriesStore.getRepositories());
                        break;
                }
                break;
            case UsersStore.STORE_ID:
                switch (change.getRxAction().getType()) {
                    case Actions.GET_USER:
                        showUserFragment((String) change.getRxAction().getData().get(Keys.ID));
                }
        }
    }

    @Override
    public void onRxError(@NonNull RxError error) {
        setLoadingFrame(false);
        Throwable throwable = error.getThrowable();
        if (throwable != null) {
            Snackbar.make(coordinatorLayout, "An error ocurred", Snackbar.LENGTH_INDEFINITE).setAction("Retry", v -> HybApp.get(this).getGitHubActionCreator().retry(error.getAction())).show();
            throwable.printStackTrace();
        } else {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRxViewRegistered() {
        // If there is any fragment that needs to register store changes we can do it here
    }

    @Override
    public void onRxViewUnRegistered() {
        // If there is any fragment that has registered for store changes we can unregister now
    }

    @Override
    public List<RxStore> getRxStoreListToRegister() {
        repositoriesStore = RepositoriesStore.get(HybApp.get(this).getRxFlux().getDispatcher());
        usersStore = UsersStore.get(HybApp.get(this).getRxFlux().getDispatcher());
        return Arrays.asList(repositoriesStore, usersStore);
    }

    @Override
    public List<RxStore> getRxStoreListToUnRegister() {
        return null;
    }

    private void showUserFragment(String id) {

        UserFragment user_fragment = UserFragment.newInstance(id);
//        getFragmentManager().beginTransaction().replace(R.id.root, user_fragment).addToBackStack(null).commit();
    }

    private void setLoadingFrame(boolean show) {
        progress_loading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void refresh() {
        setLoadingFrame(true);
        HybApp.get(this).getGitHubActionCreator().getPublicRepositories();
    }

    @Override
    public void onClicked(GitHubRepo repo) {
        String login = repo.getOwner().getLogin();
        if (usersStore.getUser(login) != null) {
            showUserFragment(login);
            return;
        }
        setLoadingFrame(true);
//        HybApp.get(this).getGitHubActionCreator().getUserDetails(login);
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }
}
