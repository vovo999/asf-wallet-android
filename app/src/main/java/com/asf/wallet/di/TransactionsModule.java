package com.asf.wallet.di;

import com.asf.wallet.interact.BuildConfigDefaultTokenProvider;
import com.asf.wallet.interact.FetchTokensInteract;
import com.asf.wallet.interact.FetchTransactionsInteract;
import com.asf.wallet.interact.FindDefaultNetworkInteract;
import com.asf.wallet.interact.FindDefaultWalletInteract;
import com.asf.wallet.interact.GetDefaultWalletBalance;
import com.asf.wallet.repository.EthereumNetworkRepositoryType;
import com.asf.wallet.repository.TokenLocalSource;
import com.asf.wallet.repository.TokenRepository;
import com.asf.wallet.repository.TokenRepositoryType;
import com.asf.wallet.repository.TransactionLocalSource;
import com.asf.wallet.repository.TransactionRepositoryType;
import com.asf.wallet.repository.WalletRepositoryType;
import com.asf.wallet.router.ExternalBrowserRouter;
import com.asf.wallet.router.ManageWalletsRouter;
import com.asf.wallet.router.MyAddressRouter;
import com.asf.wallet.router.MyTokensRouter;
import com.asf.wallet.router.SendRouter;
import com.asf.wallet.router.SettingsRouter;
import com.asf.wallet.router.TransactionDetailRouter;
import com.asf.wallet.service.TickerService;
import com.asf.wallet.service.TokenExplorerClientType;
import com.asf.wallet.viewmodel.TransactionsViewModelFactory;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module class TransactionsModule {
  @Provides TransactionsViewModelFactory provideTransactionsViewModelFactory(
      FindDefaultNetworkInteract findDefaultNetworkInteract,
      FindDefaultWalletInteract findDefaultWalletInteract,
      FetchTransactionsInteract fetchTransactionsInteract, ManageWalletsRouter manageWalletsRouter,
      SettingsRouter settingsRouter, SendRouter sendRouter,
      TransactionDetailRouter transactionDetailRouter, MyAddressRouter myAddressRouter,
      MyTokensRouter myTokensRouter, ExternalBrowserRouter externalBrowserRouter,
      FetchTokensInteract fetchTokensInteract) {
    return new TransactionsViewModelFactory(findDefaultNetworkInteract, findDefaultWalletInteract,
        fetchTransactionsInteract, manageWalletsRouter, settingsRouter, sendRouter,
        transactionDetailRouter, myAddressRouter, myTokensRouter, externalBrowserRouter,
        fetchTokensInteract);
  }

  @Provides FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
    return new FetchTokensInteract(tokenRepository, new BuildConfigDefaultTokenProvider());
  }

  @Provides FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
      EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
    return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
  }

  @Provides FindDefaultWalletInteract provideFindDefaultWalletInteract(
      WalletRepositoryType walletRepository) {
    return new FindDefaultWalletInteract(walletRepository);
  }

  @Provides FetchTransactionsInteract provideFetchTransactionsInteract(
      TransactionRepositoryType transactionRepository) {
    return new FetchTransactionsInteract(transactionRepository);
  }

  @Provides GetDefaultWalletBalance provideGetDefaultWalletBalance(
      WalletRepositoryType walletRepository,
      EthereumNetworkRepositoryType ethereumNetworkRepository) {
    return new GetDefaultWalletBalance(walletRepository, ethereumNetworkRepository);
  }

  @Provides ManageWalletsRouter provideManageWalletsRouter() {
    return new ManageWalletsRouter();
  }

  @Provides SettingsRouter provideSettingsRouter() {
    return new SettingsRouter();
  }

  @Provides SendRouter provideSendRouter() {
    return new SendRouter();
  }

  @Provides TransactionDetailRouter provideTransactionDetailRouter() {
    return new TransactionDetailRouter();
  }

  @Provides MyAddressRouter provideMyAddressRouter() {
    return new MyAddressRouter();
  }

  @Provides MyTokensRouter provideMyTokensRouter() {
    return new MyTokensRouter();
  }

  @Provides ExternalBrowserRouter provideExternalBrowserRouter() {
    return new ExternalBrowserRouter();
  }

  @Provides TokenRepository provideTokenRepository(OkHttpClient okHttpClient,
      EthereumNetworkRepositoryType ethereumNetworkRepository,
      WalletRepositoryType walletRepository, TokenExplorerClientType tokenExplorerClientType,
      TokenLocalSource tokenLocalSource, TransactionLocalSource inDiskCache,
      TickerService tickerService) {
    return new TokenRepository(okHttpClient, ethereumNetworkRepository, walletRepository,
        tokenExplorerClientType, tokenLocalSource, inDiskCache, tickerService);
  }
}
