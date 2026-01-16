package io.github.sps4j.core.load;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.Sps4jPlugin;

import java.util.Map;

public class TestPluginImpl implements Sps4jPlugin {
    private boolean loaded = false;

    @Override
    public void onLoad(Map<String, Object> conf, MetaInfo metaInfo) {
        this.loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
