package com.asf.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import com.asf.wallet.C;
import com.asf.wallet.entity.ErrorEnvelope;
import com.asf.wallet.entity.NetworkInfo;
import com.asf.wallet.entity.Token;
import com.asf.wallet.entity.Transaction;
import com.asf.wallet.entity.Wallet;
import com.asf.wallet.interact.FetchTokensInteract;
import com.asf.wallet.interact.FetchTransactionsInteract;
import com.asf.wallet.interact.FindDefaultNetworkInteract;
import com.asf.wallet.interact.FindDefaultWalletInteract;
import com.asf.wallet.router.ExternalBrowserRouter;
import com.asf.wallet.router.ManageWalletsRouter;
import com.asf.wallet.router.MyAddressRouter;
import com.asf.wallet.router.MyTokensRouter;
import com.asf.wallet.router.SendRouter;
import com.asf.wallet.router.SettingsRouter;
import com.asf.wallet.router.TransactionDetailRouter;
import com.asf.wallet.token.Erc20Token;
import com.asf.wallet.util.TokenInfoFactory;
import io.reactivex.Observable;

public class TransactionsViewModel extends BaseViewModel {
  private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;
  private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
  private static final String TAG = TransactionsViewModel.class.getSimpleName();
  private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
  private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
  private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
  private final MutableLiveData<Token> defaultWalletBalance = new MutableLiveData<>();
  private final FindDefaultNetworkInteract findDefaultNetworkInteract;
  private final FindDefaultWalletInteract findDefaultWalletInteract;
  private final FetchTransactionsInteract fetchTransactionsInteract;
  private final ManageWalletsRouter manageWalletsRouter;
  private final SettingsRouter settingsRouter;
  private final SendRouter sendRouter;
  private final TransactionDetailRouter transactionDetailRouter;
  private final MyAddressRouter myAddressRouter;
  private final MyTokensRouter myTokensRouter;
  private final ExternalBrowserRouter externalBrowserRouter;
  private final FetchTokensInteract fetchTokensInteract;
  private Handler handler = new Handler();
  private final Runnable startFetchTransactionsTask = () -> this.fetchTransactions(false);
  private final Runnable startGetBalanceTask = this::getBalance;

  TransactionsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
      FindDefaultWalletInteract findDefaultWalletInteract,
      FetchTransactionsInteract fetchTransactionsInteract, ManageWalletsRouter manageWalletsRouter,
      SettingsRouter settingsRouter, SendRouter sendRouter,
      TransactionDetailRouter transactionDetailRouter, MyAddressRouter myAddressRouter,
      MyTokensRouter myTokensRouter, ExternalBrowserRouter externalBrowserRouter,
      FetchTokensInteract fetchTokensInteract) {
    this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    this.findDefaultWalletInteract = findDefaultWalletInteract;
    this.fetchTransactionsInteract = fetchTransactionsInteract;
    this.manageWalletsRouter = manageWalletsRouter;
    this.settingsRouter = settingsRouter;
    this.sendRouter = sendRouter;
    this.transactionDetailRouter = transactionDetailRouter;
    this.myAddressRouter = myAddressRouter;
    this.myTokensRouter = myTokensRouter;
    this.externalBrowserRouter = externalBrowserRouter;
    this.fetchTokensInteract = fetchTokensInteract;
  }

  @Override protected void onCleared() {
    super.onCleared();

    handler.removeCallbacks(startFetchTransactionsTask);
    handler.removeCallbacks(startGetBalanceTask);
  }

  public LiveData<NetworkInfo> defaultNetwork() {
    return defaultNetwork;
  }

  public LiveData<Wallet> defaultWallet() {
    return defaultWallet;
  }

  public LiveData<Transaction[]> transactions() {
    return transactions;
  }

  public LiveData<Token> defaultWalletBalance() {
    return defaultWalletBalance;
  }

  public void prepare() {
    progress.postValue(true);
    disposable = findDefaultNetworkInteract.find()
        .subscribe(this::onDefaultNetwork, this::onError);
  }

  public void fetchTransactions(boolean shouldShowProgress) {
    handler.removeCallbacks(startFetchTransactionsTask);
    progress.postValue(shouldShowProgress);
        /*For specific address use: new Wallet("0x60f7a1cbc59470b74b1df20b133700ec381f15d3")*/
    Observable<Transaction[]> fetch = fetchTransactionsInteract.fetch(defaultWallet.getValue());
    fetch.subscribe(this::onTransactions, this::onError, this::onTransactionsFetchCompleted);
  }

  private void getBalance() {
    fetchTokensInteract.fetchDefaultToken(defaultWallet.getValue())
        .subscribe(token -> {
          defaultWalletBalance.postValue(token);
          handler.removeCallbacks(startGetBalanceTask);
          handler.postDelayed(startGetBalanceTask, GET_BALANCE_INTERVAL);
        }, throwable -> {
          Log.w(TAG, "getBalance: ", throwable);
          handler.removeCallbacks(startGetBalanceTask);
          handler.postDelayed(startGetBalanceTask, GET_BALANCE_INTERVAL);
        });
  }

  private void onDefaultNetwork(NetworkInfo networkInfo) {
    defaultNetwork.postValue(networkInfo);
    disposable = findDefaultWalletInteract.find()
        .subscribe(this::onDefaultWallet, this::onError);
  }

  private void onDefaultWallet(Wallet wallet) {
    defaultWallet.setValue(wallet);
    getBalance();
    fetchTransactions(true);
  }

  private void onTransactions(Transaction[] transactions) {
    this.transactions.setValue(transactions);
    Boolean last = progress.getValue();
    if (transactions != null && transactions.length > 0 && last != null && last) {
      progress.postValue(true);
    }
  }

  private void onTransactionsFetchCompleted() {
    progress.postValue(false);
    Transaction[] transactions = this.transactions.getValue();
    if (transactions == null || transactions.length == 0) {
      error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "empty collection"));
    }
    handler.postDelayed(startFetchTransactionsTask, FETCH_TRANSACTIONS_INTERVAL);
  }

  public void showWallets(Context context) {
    manageWalletsRouter.open(context, false);
  }

  public void showSettings(Context context) {
    settingsRouter.open(context);
  }

  public void showSend(Context context) {
    sendRouter.open(context, TokenInfoFactory.getTokenInfo(Erc20Token.APPC));
  }

  public void showDetails(Context context, Transaction transaction) {
    transactionDetailRouter.open(context, transaction);
  }

  public void showMyAddress(Context context) {
    myAddressRouter.open(context, defaultWallet.getValue());
  }

  public void showTokens(Context context) {
    myTokensRouter.open(context, defaultWallet.getValue());
  }

  public void pause() {
    handler.removeCallbacks(startFetchTransactionsTask);
    handler.removeCallbacks(startGetBalanceTask);
  }

  public void openDeposit(Context context, Uri uri) {
    externalBrowserRouter.open(context, uri);
  }
}
