package com.asf.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.net.Uri;
import com.asf.wallet.entity.Address;
import com.asf.wallet.entity.GasSettings;
import com.asf.wallet.entity.TransactionBuilder;
import com.asf.wallet.entity.Wallet;
import com.asf.wallet.interact.FetchGasSettingsInteract;
import com.asf.wallet.interact.FindDefaultWalletInteract;
import com.asf.wallet.router.ConfirmationRouter;
import com.asf.wallet.router.Result;
import com.asf.wallet.util.QRUri;
import com.asf.wallet.util.TransferParser;
import com.google.android.gms.vision.barcode.Barcode;
import java.math.BigDecimal;
import org.web3j.utils.Numeric;

public class SendViewModel extends BaseViewModel {
  private final MutableLiveData<String> symbol = new MutableLiveData<>();
  private final MutableLiveData<String> address = new MutableLiveData<>();
  private final MutableLiveData<BigDecimal> amount = new MutableLiveData<>();
  private final MutableLiveData<Result> transactionSucceed = new MutableLiveData<>();
  private final FindDefaultWalletInteract findDefaultWalletInteract;
  private final FetchGasSettingsInteract fetchGasSettingsInteract;
  private final ConfirmationRouter confirmationRouter;
  private final TransferParser transferParser;
  private TransactionBuilder transactionBuilder;

  SendViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
      FetchGasSettingsInteract fetchGasSettingsInteract, ConfirmationRouter confirmationRouter,
      TransferParser transferParser) {
    this.findDefaultWalletInteract = findDefaultWalletInteract;
    this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    this.confirmationRouter = confirmationRouter;
    this.transferParser = transferParser;
  }

  public void init(TransactionBuilder transactionBuilder, Uri data) {
    if (transactionBuilder != null) {
      this.transactionBuilder = transactionBuilder;
      symbol.postValue(transactionBuilder.symbol());
      fetchGasSettingsInteract.fetch(transactionBuilder.shouldSendToken())
          .subscribe(this::onGasSettings, this::onError);

      disposable = findDefaultWalletInteract.find()
          .subscribe(this::onDefaultWallet, this::onError);
    } else {
      transferParser.parse(data.toString())
          .flatMapObservable(transaction -> {
            this.transactionBuilder = transaction;
            symbol.postValue(transaction.symbol());
            address.postValue(transaction.toAddress());
            amount.postValue(transaction.amount());
            return fetchGasSettingsInteract.fetch(transaction.shouldSendToken())
                .doOnSuccess(this::onGasSettings)
                .flatMap(gasSettings -> findDefaultWalletInteract.find()
                    .doOnSuccess(this::onDefaultWallet))
                .flatMapObservable(wallet -> confirmationRouter.getTransactionResult()
                    .doOnNext(transactionSucceed::postValue));
          })
          .subscribe(wallet -> {
          }, this::onError);
    }
  }

  public MutableLiveData<Result> onTransactionSucceed() {
    return transactionSucceed;
  }

  public MutableLiveData<BigDecimal> amount() {
    return amount;
  }

  public LiveData<String> symbol() {
    return symbol;
  }

  public LiveData<String> toAddress() {
    return address;
  }

  public boolean setToAddress(String toAddress) {
    if (Address.isAddress(toAddress)) {
      transactionBuilder.toAddress(toAddress);
      return true;
    } else {
      return false;
    }
  }

  public boolean setAmount(String amount) {
    try {
      BigDecimal value = new BigDecimal(amount);
      transactionBuilder.amount(value);
      this.amount.postValue(value);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean extractFromQR(Barcode barcode) {
    QRUri qrUrl = QRUri.parse(barcode.displayValue);
    if (qrUrl != null) {
      transactionBuilder.toAddress(qrUrl.getAddress());
      if (qrUrl.getParameter("data") != null) {
        transactionBuilder.data(
            Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(qrUrl.getParameter("data"))));
      }
      address.postValue(qrUrl.getAddress());
      return true;
    } else {
      return false;
    }
  }

  public void openConfirmation(Activity activity) {
    confirmationRouter.open(activity, transactionBuilder);
  }

  private void onGasSettings(GasSettings gasSettings) {
    transactionBuilder.gasSettings(gasSettings);
  }

  private void onDefaultWallet(Wallet wallet) {
    transactionBuilder.fromAddress(wallet.address);
  }

  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    return confirmationRouter.onActivityResult(requestCode, resultCode, data);
  }
}
