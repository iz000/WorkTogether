package jang.worktogether.chatting;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import jang.worktogether.Utils.PacketUtil;

public class Packet implements Serializable {

    String protocol;
    String body = "";

    public Packet(String protocol){
        this.protocol = protocol;
    }

    public void addData(String... data){
        this.body = "";
        if(data.length >= 1) {
            for (int i = 0; i < data.length - 1; i++) {
                this.body += (data[i] + "|");
            }
            this.body += data[data.length - 1];
        }
    }

    public byte[] toByteArray(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if(this.body.equals("")){
                baos.write(protocol.getBytes("utf-8"));
                baos.write(PacketUtil.intToByteArray(0));
            }
            else{
                byte[] body = this.body.getBytes("utf-8");
                baos.write(protocol.getBytes("utf-8"));
                baos.write(PacketUtil.intToByteArray(body.length));
                baos.write(body);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return baos.toByteArray();
    }
}
