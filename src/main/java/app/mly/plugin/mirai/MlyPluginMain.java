package app.mly.plugin.mirai;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import net.mamoe.mirai.console.data.AutoSavePluginDataHolder;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.File;

/**
 * 茉莉云插件 for Mirai
 *
 * @author xggz <yyimba@qq.com>
 * @since 2021/10/21 15:31
 */
public final class MlyPluginMain extends JavaPlugin implements AutoSavePluginDataHolder {

    public static final MlyPluginMain INSTANCE = new MlyPluginMain();

    private MlyPluginMain() {
        super(new JvmPluginDescriptionBuilder("app.mly.plugin.mirai", "0.1.0")
                .name("MlyApp")
                .info("智能聊天插件，基于茉莉云机器人Api，支持天气查询、手机号归属地查询、翻译功能等。")
                .author("xggz")
                .build());
    }

    @Override
    public void onEnable() {
        getLogger().info("茉莉云插件 for Mirai已启用");

        reloadPluginConfig(MlyPluginConfig.INSTANCE);

        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        eventChannel.subscribeAlways(GroupMessageEvent.class, g -> {
            handlerGroupMessage(g);
        });
        eventChannel.subscribeAlways(FriendMessageEvent.class, f -> {
            handlerFriendMessage(f);
        });
    }

    /**
     * 验证Api配置是否正常
     *
     * @param contact
     * @return
     */
    private Boolean verifyApiConfig(Contact contact) {
        if (StrUtil.isBlank(MlyPluginConfig.INSTANCE.apiKey.get())) {
            contact.sendMessage("请配置茉莉云插件的apiKey");
            return Boolean.FALSE;
        } else if (StrUtil.isBlank(MlyPluginConfig.INSTANCE.apiSecret.get())) {
            contact.sendMessage("请配置茉莉云插件的apiSecret");
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 处理好友消息
     *
     * @param friendMessageEvent
     */
    private void handlerFriendMessage(FriendMessageEvent friendMessageEvent) {
        Friend friend = friendMessageEvent.getFriend();
        if (verifyApiConfig(friend)) {
            JSONArray dataArray = getMlyAppReply(friendMessageEvent.getMessage().contentToString(), 1, String.valueOf(friend.getId()), friend.getNick(), null, null);
            messageSender(dataArray, friend);
        }
    }

    /**
     * 处理群消息
     *
     * @param groupMessageEvent
     */
    private void handlerGroupMessage(GroupMessageEvent groupMessageEvent) {
        Group group = groupMessageEvent.getGroup();
        if (verifyApiConfig(group)) {
            Member member = groupMessageEvent.getSender();
            JSONArray dataArray = getMlyAppReply(groupMessageEvent.getMessage().contentToString(), 2, String.valueOf(member.getId()), member.getNick(), String.valueOf(group.getId()), group.getName());
            messageSender(dataArray, group);
        }
    }

    /**
     * 消息发送处理
     *
     * @param dataArray
     * @param contact
     */
    private void messageSender(JSONArray dataArray, Contact contact) {
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject data = (JSONObject) dataArray.get(i);
            Integer typed = data.getInt("typed");

            switch (typed) {
                case 1:
                    contact.sendMessage(data.getStr("content"));
                    break;
                case 2:
                    File destFile = FileUtil.file("mly-temp/" + data.getStr("content"));
                    if (destFile == null || !destFile.exists()) {
                        getLogger().info("DEST:"+destFile.toString());
                        HttpUtil.downloadFileFromUrl("https://files.molicloud.com/"+data.getStr("content"), destFile);
                    }
                    ExternalResource externalImage = ExternalResource.create(destFile);
                    Image image = contact.uploadImage(externalImage);
                    contact.sendMessage(image);
                    break;
                default:
                    contact.sendMessage("文件：" + data.getStr("remark"));
                    break;
            }
        }
    }

    /**
     * 获取茉莉云智能回复
     *
     * Api-Key和Api-Secret从https://mly.app的机器人详情里面获取
     *
     * @param content
     * @param type
     * @param from
     * @param fromName
     * @param to
     * @param toName
     * @return
     */
    private JSONArray getMlyAppReply(String content, Integer type, String from, String fromName, String to, String toName) {
        JSONObject body = new JSONObject();
        body.set("content", content);
        body.set("type", type);  // 1好友消息，2群消息
        body.set("from", from);
        body.set("fromName", fromName);
        if (type == 2) {
            body.set("to", to);
            body.set("toName", toName);
        }

        String result = HttpRequest.post("https://i.mly.app/reply")
                .header("Api-Key", MlyPluginConfig.INSTANCE.apiKey.get())
                .header("Api-Secret", MlyPluginConfig.INSTANCE.apiSecret.get())
                .body(body.toString())
                .execute()
                .body();

        JSONObject info = new JSONObject(result);
        if (!"00000".equals(info.getStr("code"))) {
            throw new RuntimeException("Code：" + info.getStr("code") + "，Message：" + info.getStr("message"));
        }

        return info.getJSONArray("data");
    }
}
