package edu.cnm.deepdive.codebreaker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import edu.cnm.deepdive.codebreaker.R;
import edu.cnm.deepdive.codebreaker.adapter.HistoryAdapter.Holder;
import edu.cnm.deepdive.codebreaker.databinding.ItemHistoryBinding;
import edu.cnm.deepdive.codebreaker.model.pojo.GameWithGuesses;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<Holder> {

  private final List<GameWithGuesses> games;
  private final LayoutInflater inflater;
  private final DateFormat dateFormat;
  private final DateFormat timeFormat;
  private final String startFormat;
  private final String attemptsFormat;
  private final String elapsedTimeFormat;

  public HistoryAdapter(@NonNull Context context) {
    this.games = new ArrayList<>();
    inflater = LayoutInflater.from(context);
    dateFormat = android.text.format.DateFormat.getDateFormat(context);
    timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    startFormat = context.getString(R.string.start_format);
    attemptsFormat = context.getString(R.string.attempts_format);
    elapsedTimeFormat = context.getString(R.string.elapsed_time_format);
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(ItemHistoryBinding.inflate(inflater, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(position);
  }

  @Override
  public int getItemCount() {
    return games.size();
  }

  public List<GameWithGuesses> getGames() {
    return games;
  }

  class Holder extends RecyclerView.ViewHolder {

    private final ItemHistoryBinding binding;

    private Holder(ItemHistoryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(int position) {
      GameWithGuesses game = games.get(position);
      binding.start.setText(String.format(startFormat,
          dateFormat.format(game.getCreated()), timeFormat.format(game.getCreated())));
      binding.attempts.setText(String.format(attemptsFormat, game.getGuesses().size()));
      long start = game.getGuesses().get(0).getCreated().getTime();
      long end = game.getGuesses().get(game.getGuesses().size() - 1).getCreated().getTime();
      int seconds = (int) Math.round((end - start) / 1000.0);
      int minutes = seconds / 60;
      seconds %= 60;
      int hours = minutes / 60;
      minutes %= 60;
      binding.elapsedTime.setText(String.format(elapsedTimeFormat, hours, minutes, seconds));
    }

  }

}