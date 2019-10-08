package mx.wsFerryTCP.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mx.utils.ByteUtils;
import mx.utils.Json;

import java.io.UnsupportedEncodingException;

public abstract class Message {

    /**
     * 登录消息
     * type 00
     */
    public static final class LoginMessage extends Message{
        public final String userName;
        public final String password;

        /**
         * 注册感兴趣的监听端口
         */
        public final int listenPort;

        public LoginMessage(int listenPort,String userName, String password) {
            this.listenPort = listenPort;
            this.userName = userName;
            this.password = password;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public int getListenPort() {
            return listenPort;
        }


        @Override
        public byte[] marshal() {
            try {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(loginMessageType);
                buf.writeInt(listenPort);
                //字符串拼接
                buf.writeBytes((userName+"||"+password).getBytes("UTF-8"));
                return ByteUtils.getBytes(buf);
            } catch (UnsupportedEncodingException e) {
            }

            return null;
        }
    }

    /**
     * tcp listen端accept连接的消息,会发给注册的ferry客户端
     * type 31
     */
    public static final class ConnectActiveMessage extends Message{
        /**
         * 会将tcp listen端的connectId发送到ferry客户端
         */
        public final int connectId;

        public ConnectActiveMessage(int connectId) {
            this.connectId = connectId;
        }

        public long getConnectId() {
            return connectId;
        }

        @Override
        public byte[] marshal() {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(connectActiveMessageType);
            buf.writeInt(connectId);
            return ByteUtils.getBytes(buf);
        }

        static ConnectActiveMessage _unmarshal(byte[] bs){
            ByteBuf buf = Unpooled.wrappedBuffer(bs);
            buf.readByte();
            int connectId = buf.readInt();

            return new ConnectActiveMessage(connectId);
        }
    }

    /**
     * 两边的连接断掉消息,发送给对方
     * type 61
     */
    public static final class ConnectInactiveMessage extends Message{
        public final int connectId;

        /**
         * 一旦接收到这个消息,会断掉本地建立的连接
         */
        public ConnectInactiveMessage(int connectId) {
            this.connectId = connectId;
        }

        public long getConnectId() {
            return connectId;
        }

        @Override
        public byte[] marshal() {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(connectInactiveMessageType);
            buf.writeInt(connectId);
            return ByteUtils.getBytes(buf);
        }

        static ConnectInactiveMessage _unmarshal(byte[] bs){
           ByteBuf buf = Unpooled.wrappedBuffer(bs);
           buf.readByte();
           int connectId = buf.readInt();

           return new ConnectInactiveMessage(connectId);
        }
    }


    /**
     * 登录响应
     * type 01
     */
    public static final class LoginResponseMessage extends Message{
        public final String error;

        /**
         * 如果有错误消息,将返回error,如果没有错误,error为null
         */
        public LoginResponseMessage(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        @Override
        public byte[] marshal() {
            try {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(loginResponseMessageType);
                if(error!=null && error.length()>0){
                    buf.writeBytes(error.getBytes("UTF-8"));
                }
                return ByteUtils.getBytes(buf);
            } catch (UnsupportedEncodingException e) {
            }

            return null;
        }

        static LoginResponseMessage _unmarshal(byte[] bs){
            try {
                ByteBuf buf = Unpooled.wrappedBuffer(bs);
                buf.readByte();

                String error = null;

                int l;
                if((l = bs.length-1)>0){
                    byte[] strBs  = new byte[l];
                    buf.readBytes(strBs);
                    error = new String(strBs,"UTF-8");
                }

                return new LoginResponseMessage(error);
            } catch (UnsupportedEncodingException e) {
            }

            return null;
        }
    }


    /**
     * 数据传输消息
     * type 30
     */
    public static final class DataMessage extends Message{
        public final int   connectId;
        public final byte[] data;

        public DataMessage(int connectId,byte[] data) {
            this.connectId = connectId;
            this.data = data;
        }

        public long getConnectId() {
            return connectId;
        }

        public byte[] getData() {
            return data;
        }

        @Override
        public byte[] marshal() {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(dataMessageType);
            buf.writeInt(connectId);
            buf.writeBytes(data);
            return ByteUtils.getBytes(buf);
        }

        static DataMessage _unmarshal(byte[] bs){
            ByteBuf buf = Unpooled.wrappedBuffer(bs);
            buf.readByte();
            int connectId = buf.readInt();
            byte[] data  = new byte[bs.length-5];
            buf.readBytes(data);

            return new DataMessage(connectId,data);
        }
    }

    /**
     * 序列化
     */
    public abstract byte[] marshal();

    public static final byte loginMessageType = 0;
    public static final byte connectActiveMessageType = 31;
    public static final byte connectInactiveMessageType = 61;
    public static final byte loginResponseMessageType = 01;
    public static final byte dataMessageType = 30;


    /**
     * 反序列化
     */
    public static Message unmarshal(byte[] bs){
        int type = (int)(bs[0]);
        switch (type){
            case loginMessageType:
                throw new RuntimeException("在ferry中没有反序列化登录消息的可能性");
            case connectActiveMessageType:
                return ConnectActiveMessage._unmarshal(bs);
            case connectInactiveMessageType:
                return ConnectInactiveMessage._unmarshal(bs);
            case loginResponseMessageType:
                return LoginResponseMessage._unmarshal(bs);
            case dataMessageType:
                return DataMessage._unmarshal(bs);

            default:
                throw new RuntimeException("反序列化的时候遇到未知的类型"+type);
        }
    }

    static String marshalToHex(Message message){
        byte[] bs = message.marshal();
        String hex = ByteUtils.toHexString(bs);
        System.out.println(hex);
        return hex;
    }

    static void unMarshalTest(String hex){
        byte[] bs = ByteUtils.toBytes(hex);
        Message message = unmarshal(bs);
        System.out.println(message.getClass()+"/"+new Json(message));
    }

    public static void main(String[] args) {
        LoginMessage loginMessage = new LoginMessage(19090,"admin","Oneplus1=two");
        String hex = marshalToHex(loginMessage);
        System.out.println("---登录消息:"+hex);

        System.out.println("---断连接消息:");
        ConnectInactiveMessage connectInactiveMessage = new ConnectInactiveMessage(11345);
        hex = marshalToHex(connectInactiveMessage);
        unMarshalTest(hex);

        System.out.println("---登录响应消息:");
        LoginResponseMessage loginResponseMessage = new LoginResponseMessage("已经有另外的账户登录了");
        hex = marshalToHex(loginResponseMessage);
        unMarshalTest(hex);

        System.out.println("---数据传输消息:");
        DataMessage dataMessage = new DataMessage(919183,new byte[]{0x00,(byte)0x99,(byte)0xff,(byte)0x80,0x7f});
        hex = marshalToHex(dataMessage);
        unMarshalTest(hex);

        //--------------------------------------------------------
        System.out.println("##########验证golang的结果##########");
        unMarshalTest("01 E5 B7 B2 E7 BB 8F E6 9C 89 E5 8F A6 E5 A4 96 E7 9A 84 E8 B4 A6 E6 88 B7 E7 99 BB E5 BD 95 E4 BA 86");
    }
}
