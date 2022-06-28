/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mythtv.leanfront.exoplayer2.source;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorInput;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.upstream.DataReader;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import org.mythtv.leanfront.MyApplication;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.ui.MainFragment;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@link ProgressiveMediaExtractor} built on top of {@link Extractor} instances, whose
 * implementation classes are bundled in the app.
 */
public final class BundledExtractorsAdapter implements ProgressiveMediaExtractor {

  private final ExtractorsFactory extractorsFactory;

  @Nullable private Extractor extractor;
  @Nullable private ExtractorInput extractorInput;

  /**
   * Creates a holder that will select an extractor and initialize it using the specified output.
   *
   * @param extractorsFactory The {@link ExtractorsFactory} providing the extractors to choose from.
   */
  public BundledExtractorsAdapter(ExtractorsFactory extractorsFactory) {
    this.extractorsFactory = extractorsFactory;
  }

  @Override
  public void init(
      DataReader dataReader,
      Uri uri,
      Map<String, List<String>> responseHeaders,
      long position,
      long length,
      ExtractorOutput output)
      throws IOException {
    ExtractorInput extractorInput = new DefaultExtractorInput(dataReader, position, length);
    this.extractorInput = extractorInput;
    if (extractor != null) {
      return;
    }
    Extractor[] extractors = extractorsFactory.createExtractors(uri, responseHeaders);
    if (extractors.length == 1) {
      this.extractor = extractors[0];
    } else {
      for (Extractor extractor : extractors) {
        try {
          if (extractor.sniff(extractorInput)) {
            this.extractor = extractor;
            break;
          }
        } catch (EOFException e) {
          // Do nothing.
        } finally {
          Assertions.checkState(this.extractor != null || extractorInput.getPosition() == position);
          extractorInput.resetPeekPosition();
        }
      }
      if (extractor == null) {
        throw new UnrecognizedInputFormatException(
            "None of the available extractors ("
                + Util.getCommaDelimitedSimpleClassNames(extractors)
                + ") could read the stream.",
            Assertions.checkNotNull(uri));
      }
    }
    extractor.init(output);
  }

  @Override
  public void release() {
    if (extractor != null) {
      extractor.release();
      extractor = null;
    }
    extractorInput = null;
  }

  @Override
  public void disableSeekingOnMp3Streams() {
    if (extractor instanceof Mp3Extractor) {
      ((Mp3Extractor) extractor).disableSeeking();
    }
  }

  @Override
  public long getCurrentInputPosition() {
    return extractorInput != null ? extractorInput.getPosition() : C.POSITION_UNSET;
  }

  @Override
  public void seek(long position, long seekTimeUs) {
    Assertions.checkNotNull(extractor).seek(position, seekTimeUs);
  }

  @Override
  public int read(PositionHolder positionHolder) throws IOException {
    int ret;
    try {
      ret = Assertions.checkNotNull(extractor)
              .read(Assertions.checkNotNull(extractorInput), positionHolder);
    }
    catch(ArrayIndexOutOfBoundsException ex) {
      handleError(ex);
      ret = Extractor.RESULT_CONTINUE;
    }
    return ret;
  }

  private static final String TAG = "lfe";
  private static final String CLASS = "BundledExtractorsAdapter";
  private int errorCount;

  void handleError(ArrayIndexOutOfBoundsException ex) {
    ++errorCount;
    Log.e(TAG, CLASS + " ArrayIndexOutOfBoundsException in BundledExtractorsAdapter.read, "
      + errorCount + " occurrences", ex);
    if ("true".equals(Settings.getString("pref_error_toast"))) {
        Context context = MyApplication.getAppContext();
        if (context == null)
          return;
        MainFragment.ToastShower toastShower = new MainFragment.ToastShower(context, R.string.pberror_extractor_array, Toast.LENGTH_LONG);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(toastShower);
    }
    // This normally occurs twice in a playback. Fail if it happens more than 10 times
    if (errorCount > 10)
      throw ex;
  }
}
