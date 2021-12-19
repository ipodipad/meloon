package com.naman14.timber.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.naman14.timber.R;
import com.naman14.timber.utils.PreferencesUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by naman on 29/10/16.
 */
public class DonateActivity extends BaseThemedActivity implements BillingProcessor.IBillingHandler {

    private static final String DONATION_1 = "naman14.timber.donate_1";
    private static final String DONATION_2= "naman14.timber.donate_2";


    private boolean readyToPurchase = false;
    BillingProcessor bp;

    private LinearLayout productListView;
    private ProgressBar progressBar;
    private TextView status;

    private String action = "support";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("빅복이를 도와주세요~! paypal.me/ipodipad");
        action = getIntent().getAction();

        productListView = (LinearLayout) findViewById(R.id.product_list);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        status = (TextView) findViewById(R.id.donation_status);

        if (action != null && action.equals("restore")) {
            status.setText("Restoring purchases..");
        }

        bp = new BillingProcessor(this, getString(R.string.play_billing_license_key), this);

    }

    @Override
    public void onBillingInitialized() {
        readyToPurchase = true;
        checkStatus();
        if (!(action != null && action.equals("restore")))
            getProducts();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        checkStatus();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DonateActivity.this, "Thanks for your support!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DonateActivity.this, "Unable to process purchase", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkStatus() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                List<String> owned = bp.listOwnedProducts();
                return owned != null && owned.size() != 0;
            }

            @Override
            protected void onPostExecute(Boolean b) {
                super.onPostExecute(b);
                if (b) {
                    PreferencesUtility.getInstance(DonateActivity.this).setFullUnlocked(true);
                    status.setText("Thanks for your support!");
                    if (action != null && action.equals("restore")) {
                        status.setText("Your purchases has been restored. Thanks for your support");
                        progressBar.setVisibility(View.GONE);
                    }
                    if (getSupportActionBar() != null)
                        getSupportActionBar().setTitle("Support development");
                } else {
                    if (action != null && action.equals("restore")) {
                        status.setText("No previous purchase found");
                        getProducts();
                    }
                }
            }
        }.execute();
    }

    private void getProducts() {
//supertoss://send?amount=1000&bank=%EC%9A%B0%EB%A6%AC%EC%9D%80%ED%96%89&accountNo=81912908402001&origin=qr
        new AsyncTask<Void, Void, List<SkuDetails>>() {
            @Override
            protected List<SkuDetails> doInBackground(Void... voids) {

                ArrayList<String> products = new ArrayList<>();

                products.add(DONATION_1);
                products.add(DONATION_2);

                return bp.getPurchaseListingDetails(products);
            }

            @Override
            protected void onPostExecute(List<SkuDetails> productList) {
                super.onPostExecute(productList);

                if (productList == null)
                    return;

                Collections.sort(productList, new Comparator<SkuDetails>() {
                    @Override
                    public int compare(SkuDetails skuDetails, SkuDetails t1) {
                        if (skuDetails.priceValue >= t1.priceValue)
                            return 1;
                        else if (skuDetails.priceValue <= t1.priceValue)
                            return -1;
                        else return 0;
                    }
                });
                for (int i = 0; i < productList.size(); i++) {
                    final SkuDetails product = productList.get(i);
                    View rootView = LayoutInflater.from(DonateActivity.this).inflate(R.layout.item_donate_product, productListView, false);

                    TextView detail = (TextView) rootView.findViewById(R.id.product_detail);
                    if( i == 0 ) {
                        detail.setText("페이팔로 천원 기부");
                    } else
                    {
                        detail.setText("토스로 천원 기부");
                    }
                        
                    
                    final int finalI = i;
                    rootView.findViewById(R.id.btn_donate).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final int idx = finalI;
                            if( idx == 0 ) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/ipodipad"));
                                startActivity(browserIntent);
                            }
                            else{
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("supertoss://send?amount=1000&bank=%EC%9A%B0%EB%A6%AC%EC%9D%80%ED%96%89&accountNo=81912908402001&origin=qr"));
                                startActivity(browserIntent);
                            }

                        }
                    });

                    productListView.addView(rootView);

                }
                progressBar.setVisibility(View.GONE);
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
