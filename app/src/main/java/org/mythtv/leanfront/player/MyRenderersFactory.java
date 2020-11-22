package org.mythtv.leanfront.player;

import android.content.Context;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.text.TextOutput;

import org.mythtv.leanfront.exoplayer2.text.TextRenderer;

import java.util.ArrayList;

public class MyRenderersFactory extends DefaultRenderersFactory {
    public MyRenderersFactory(Context context) {
        super(context);
    }

    @Override
    protected void buildTextRenderers(
            Context context,
            TextOutput output,
            Looper outputLooper,
            @ExtensionRendererMode int extensionRendererMode,
            ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, outputLooper));
    }

}
