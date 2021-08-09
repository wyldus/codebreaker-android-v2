package edu.cnm.deepdive.codebreaker.controller;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import edu.cnm.deepdive.codebreaker.R;
import edu.cnm.deepdive.codebreaker.adapter.HistoryAdapter;
import edu.cnm.deepdive.codebreaker.databinding.FragmentHistoryBinding;
import edu.cnm.deepdive.codebreaker.viewmodel.GameViewModel;
import edu.cnm.deepdive.codebreaker.viewmodel.NotificationsViewModel;
import java.util.function.BiConsumer;

public class HistoryFragment extends Fragment implements OnSeekBarChangeListener {

  private FragmentHistoryBinding binding;
  private GameViewModel viewModel;
  private BiConsumer<Integer, Boolean> codeLengthUpdater;
  private BiConsumer<Integer, Boolean> poolSizeUpdater;
  private HistoryAdapter adapter;

  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentHistoryBinding.inflate(inflater, container, false);
    adapter = new HistoryAdapter(getContext());
    binding.history.setAdapter(adapter);
    setupParameterChangeConsumers();
    setupParameterChangeListeners();
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    //noinspection ConstantConditions
    viewModel = new ViewModelProvider(getActivity()).get(GameViewModel.class);
    viewModel.getCodeLength().observe(getViewLifecycleOwner(), (codeLength) ->
        binding.codeLength.setProgress(codeLength));
    viewModel.getPoolSize().observe(getViewLifecycleOwner(), (poolSize) ->
        binding.poolSize.setProgress(poolSize));
    viewModel.getHistory().observe(getViewLifecycleOwner(), (games) -> {
      adapter.getGames().clear();
      adapter.getGames().addAll(games);
      adapter.notifyDataSetChanged();
    });
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    //noinspection unchecked
    ((BiConsumer<Integer, Boolean>) seekBar.getTag()).accept(progress, fromUser);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  private void setupParameterChangeConsumers() {
    codeLengthUpdater = (value, fromUser) -> {
      binding.codeLengthDisplay.setText(String.valueOf(value));
      if (fromUser) {
        viewModel.setCodeLength(value);
      }
    };
    poolSizeUpdater = (value, fromUser) -> {
      binding.poolSizeDisplay.setText(String.valueOf(value));
      if (fromUser) {
        viewModel.setPoolSize(value);
      }
    };
  }

  private void setupParameterChangeListeners() {
    Resources res = getResources();
    binding.codeLengthDisplay.setText(
        String.valueOf(res.getInteger(R.integer.code_length_pref_default)));
    binding.codeLength.setTag(codeLengthUpdater);
    binding.codeLength.setOnSeekBarChangeListener(this);
    binding.poolSizeDisplay.setText(
        String.valueOf(res.getInteger(R.integer.pool_size_pref_default)));
    binding.poolSize.setTag(poolSizeUpdater);
    binding.poolSize.setOnSeekBarChangeListener(this);
  }


}