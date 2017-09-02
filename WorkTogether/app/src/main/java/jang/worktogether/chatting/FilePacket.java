package jang.worktogether.chatting;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jang.worktogether.Utils.PacketUtil;

public class FilePacket extends Packet {

    private int fileLength;

    public FilePacket(String protocol) {
        super(protocol);
        this.fileLength = 0;
    }

    public void setFileLength(int fileLength) {
        this.fileLength = fileLength;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if(this.body.equals("")){
                baos.write(protocol.getBytes("utf-8"));
                baos.write(PacketUtil.intToByteArray(0));
                baos.write(PacketUtil.intToByteArray(fileLength));
            }
            else{
                byte[] body = this.body.getBytes("utf-8");
                baos.write(protocol.getBytes("utf-8"));
                baos.write(PacketUtil.intToByteArray(body.length));
                baos.write(PacketUtil.intToByteArray(fileLength));
                baos.write(body);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return baos.toByteArray();
    }


}
