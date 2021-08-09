package edu.cnm.deepdive.codebreaker.service;

import android.content.Context;
import androidx.lifecycle.LiveData;
import edu.cnm.deepdive.codebreaker.model.dao.GameDao;
import edu.cnm.deepdive.codebreaker.model.dao.GuessDao;
import edu.cnm.deepdive.codebreaker.model.entity.Game;
import edu.cnm.deepdive.codebreaker.model.entity.Guess;
import edu.cnm.deepdive.codebreaker.model.pojo.GameWithGuesses;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.List;

public class GameRepository {

  private final CodebreakerServiceProxy proxy;
  private final Context context;
  private final GameDao gameDao;
  private final GuessDao guessDao;

  public GameRepository(Context context) {
    this.context = context;
    proxy = CodebreakerServiceProxy.getInstance();
    CodebreakerDatabase database = CodebreakerDatabase.getInstance();
    gameDao = database.getGameDao();
    guessDao = database.getGuessDao();
  }

  public Single<Game> save(Game game) {
    return (
        (game.getId() == 0)
            ? proxy
            .startGame(game)
            .flatMap((receivedGame) -> {
              receivedGame.setPoolSize((int) receivedGame.getPool().codePoints().count());
              return gameDao
                  .insert(receivedGame)
                  .map((id) -> {
                    receivedGame.setId(id);
                    return receivedGame;
                  });
            })
            : gameDao
                .update(game)
                .map((count) -> game)
    )
        .subscribeOn(Schedulers.io());
  }

  public LiveData<GameWithGuesses> get(long id) {
    return gameDao.select(id);
  }

  public Completable remove(Game game) {
    return (
        (game.getId() == 0)
            ? Completable.complete()
            : gameDao
                .delete(game)
                .ignoreElement()
    )
        .subscribeOn(Schedulers.io());
  }

  public Single<Game> save(Game game, Guess guess) {
    return (
        (guess.getId() == 0)
            ? proxy
            .submitGuess(game.getServiceKey(), guess)
            .map((receivedGuess) -> {
              receivedGuess.setGameId(game.getId());
              return receivedGuess;
            })
            .flatMap((receivedGuess) -> {
              Single<Guess> task;
              if (receivedGuess.isSolution()) {
                game.setSolved(true);
                task = gameDao
                    .update(game)
                    .map((count) -> receivedGuess);
              } else {
                task = Single.just(receivedGuess);
              }
              return task;
            })
            .flatMap(guessDao::insert)
            .map((id) -> game)
            : guessDao
                .update(guess)
                .map((count) -> game)
    )
        .subscribeOn(Schedulers.io());
  }

  public LiveData<List<GameWithGuesses>> getScoreboardAttempts(int codeLength, int poolSize) {
    return gameDao.selectTopScoresByAttempts(codeLength, poolSize);
  }

  public LiveData<List<GameWithGuesses>> getScoreboardTime(int codeLength, int poolSize) {
    return gameDao.selectTopScoresByTime(codeLength, poolSize);
  }

  public LiveData<List<GameWithGuesses>> getHistory(int codeLength, int poolSize) {
    return gameDao.selectHistory(codeLength, poolSize);
  }

}