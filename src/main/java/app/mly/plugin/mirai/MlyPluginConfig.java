package app.mly.plugin.mirai;

import net.mamoe.mirai.console.data.Value;
import net.mamoe.mirai.console.data.java.JAutoSavePluginData;

/**
 * 茉莉云插件配置
 *
 * @author xggz <yyimba@qq.com>
 * @since 2021/10/21 17:07
 */
public class MlyPluginConfig extends JAutoSavePluginData {

    public MlyPluginConfig(String saveName) {
        super(saveName);
    }

    public static final MlyPluginConfig INSTANCE = new MlyPluginConfig("api");

    public final Value<String> apiKey = value("apiKey");
    public final Value<String> apiSecret = value("apiSecret");
}