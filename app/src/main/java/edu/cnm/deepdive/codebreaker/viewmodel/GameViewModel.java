package edu.cnm.deepdive.codebreaker.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;
import edu.cnm.deepdive.codebreaker.R;
import edu.cnm.deepdive.codebreaker.model.entity.Game;
import edu.cnm.deepdive.codebreaker.model.entity.Guess;
import edu.cnm.deepdive.codebreaker.model.pojo.GameWithGuesses;
import edu.cnm.deepdive.codebreaker.service.GameRepository;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;
import java.util.stream.Collectors;

public class GameViewModel extends AndroidViewModel implements LifecycleObserver {

  private final GameRepository repository;
  private final MutableLiveData<Long> gameId;
  private final LiveData<GameWithGuesses> game;
  private final MutableLiveData<Integer> codeLength;
  private final MutableLiveData<Integer> poolSize;
  private final MutableLiveData<Boolean> sortedByTime;
  private final LiveData<List<GameWithGuesses>> scoreboard;
  private final LiveData<List<GameWithGuesses>> history;
  private final MutableLiveData<Throwable> throwable;
  private final CompositeDisposable pending;
  private final SharedPreferences preferences;
  private final String basePool;

  public GameViewModel(@NonNull Application application) {
    super(application);
    preferences = PreferenceManager.getDefaultSharedPreferences(application);
    repository = new GameRepository(application);
    gameId = new MutableLiveData<>();
    game = Transformations.switchMap(gameId, repository::get);
    codeLength = new MutableLiveData<>(getCodeLengthPref());
    poolSize = new MutableLiveData<>(getPoolSizePref());
    sortedByTime = new MutableLiveData<>(false);
    ScoreboardFilterLiveData scoreboardTrigger =
        new ScoreboardFilterLiveData(codeLength, poolSize, sortedByTime);
    scoreboard = Transformations.switchMap(scoreboardTrigger, (params) -> params.sortedByTime
        ? repository.getScoreboardTime(params.codeLength, params.poolSize)
        : repository.getScoreboardAttempts(params.codeLength, params.poolSize));
    HistoryFilterLiveData historyTrigger =
        new HistoryFilterLiveData(codeLength, poolSize);
    history = Transformations.switchMap(historyTrigger, (params) ->
        repository.getHistory(params.codeLength, params.poolSize));
    throwable = new MutableLiveData<>();
    pending = new CompositeDisposable();
    String[] emojis = application.getResources().getStringArray(R.array.emojis);
    StringBuilder builder  = new StringBuilder();
    for (String emoji : emojis) {
      builder.append(emoji);
    }
    basePool = builder.toString();
    startGame();
  }

  public LiveData<GameWithGuesses> getGame() {
    return game;
  }

  public LiveData<Integer> getCodeLength() {
    return codeLength;
  }

  public void setCodeLength(int codeLength) {
    this.codeLength.setValue(codeLength);
  }

  public LiveData<Integer> getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize.setValue(poolSize);
  }

  public LiveData<Boolean> getSortedByTime() {
    return sortedByTime;
  }

  public void setSortedByTime(boolean sortedByTime) {
    this.sortedByTime.setValue(sortedByTime);
  }

  public LiveData<List<GameWithGuesses>> getScoreboard() {
    return scoreboard;
  }

  public LiveData<List<GameWithGuesses>> getHistory() {
    return history;
  }

  public LiveData<Throwable> getThrowable() {
    return throwable;
  }

  public void startGame() {
    throwable.setValue(null);
    Game game = new Game();
    game.setPool(getPoolPref());
    game.setLength(getCodeLengthPref());
    pending.add(
        repository
            .save(game)
            .subscribe(
                (updatedGame) -> gameId.postValue(updatedGame.getId()),
                this::handleThrowable
            )
    );
  }

  public void submitGuess(Game game, String text) {
    throwable.setValue(null);
    Guess guess = new Guess();
    guess.setText(text);
    pending.add(
        repository
            .save(game, guess)
            .subscribe(
                (ignored) -> {},
                this::handleThrowable
            )
    );
  }

  @OnLifecycleEvent(Event.ON_STOP)
  private void clearPending() {
    pending.clear();
  }

  private void handleThrowable(Throwable throwable) {
    Log.e(getClass().getName(), throwable.getMessage(), throwable);
    this.throwable.postValue(throwable);
  }

  private int getCodeLengthPref() {
    Context context = getApplication();
    Resources res = context.getResources();
    return preferences.getInt(res.getString(R.string.code_length_pref_key),
        res.getInteger(R.integer.code_length_pref_default));
  }

  private int getPoolSizePref() {
    Resources res = getApplication().getResources();
    return preferences.getInt(res.getString(R.string.pool_size_pref_key),
        res.getInteger(R.integer.pool_size_pref_default));
  }

  private String getPoolPref() {
    return basePool
        .codePoints()
        .limit(getPoolSizePref())
        .mapToObj((codePoint) -> new String(new int[]{codePoint}, 0, 1))
        .collect(Collectors.joining());
  }

  private static class ScoreboardFilterLiveData extends MediatorLiveData<ScoreboardParams> {

    @SuppressWarnings("ConstantConditions")
    public ScoreboardFilterLiveData(
        LiveData<Integer> codeLength, LiveData<Integer> poolSize, LiveData<Boolean> sortedByTime) {
      addSource(codeLength, (length) ->
          setValue(new ScoreboardParams(length, poolSize.getValue(), sortedByTime.getValue())));
      addSource(poolSize, (size) ->
          setValue(new ScoreboardParams(codeLength.getValue(), size, sortedByTime.getValue())));
      addSource(sortedByTime, (flag) ->
          setValue(new ScoreboardParams(codeLength.getValue(), poolSize.getValue(), flag)));
    }

  }

  private static class HistoryFilterLiveData extends MediatorLiveData<HistoryParams> {

    @SuppressWarnings("ConstantConditions")
    public HistoryFilterLiveData(LiveData<Integer> codeLength, LiveData<Integer> poolSize) {
      addSource(codeLength, (length) ->
          setValue(new HistoryParams(length, poolSize.getValue())));
      addSource(poolSize, (size) ->
          setValue(new HistoryParams(codeLength.getValue(), size)));
    }

  }

  private static class ScoreboardParams {

    private final int codeLength;
    private final int poolSize;
    private final boolean sortedByTime;

    private ScoreboardParams(int codeLength, int poolSize, boolean sortedByTime) {
      this.codeLength = codeLength;
      this.poolSize = poolSize;
      this.sortedByTime = sortedByTime;
    }

  }

  private static class HistoryParams {

    private final int codeLength;
    private final int poolSize;

    private HistoryParams(int codeLength, int poolSize) {
      this.codeLength = codeLength;
      this.poolSize = poolSize;
    }

  }

}