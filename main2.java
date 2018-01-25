import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;
import java.io.*;
import javax.sound.sampled.*;

class application {
	public int client_port = 48030;
	public int server_port = 38030;
	public String echo_code = "E2183";
	public String picture_code= "M6565";
	public String sound_code= "V6616";
	public DatagramSocket s;
	public InetAddress serverAdress;

	public void echo(application ap,String c) throws IOException {
		long t=0;
		long time_diff=0;
		String mes=c;
		byte[] buffer_1=new byte[mes.length()];
		byte[] buffer_2=new byte[1024];
		Vector <String> table=new Vector <String>();
		buffer_1=mes.getBytes();
		DatagramPacket packet_1=new DatagramPacket(buffer_1, 0, buffer_1.length, ap.serverAdress, ap.server_port);
		DatagramPacket packet_2=new DatagramPacket(buffer_2, 0, buffer_2.length);
		File xls = new File(c+".xls");
		FileWriter w=new FileWriter(xls);	
		long t_end=240000;
		long t_start=System.currentTimeMillis();

		while(true){	
			ap.s.send(packet_1);
			t=System.currentTimeMillis();
			ap.s.receive(packet_2);
			time_diff=System.currentTimeMillis()-t;
			Long t_2=new Long(time_diff);
			table.add(t_2.toString());
			if (System.currentTimeMillis()-t_start>=t_end)
			break;
		}
		for(int i=0; i<table.size(); i++)
			w.write(table.get(i)+"\n");
		
		w.close();	
	}	

	public void image(application ap, String cam) throws IOException{
		String msg=ap.picture_code+cam+"FLOW=ONUDP=1024";
		byte[] buffer_1=new byte[msg.length()];
		byte[] buffer_2=new byte[1024];
		buffer_1=msg.getBytes();
		DatagramPacket packet_1=new DatagramPacket(buffer_1, 0, buffer_1.length, serverAdress, server_port);
		DatagramPacket packet_2=new DatagramPacket(buffer_2, 0, buffer_2.length);
		byte[] buffer_3=new String("NEXT").getBytes();
		DatagramPacket ans=new DatagramPacket(buffer_3, 0, buffer_3.length, serverAdress, server_port);
		File jpeg = new File(cam+".jpeg");
		FileOutputStream out = new FileOutputStream(jpeg);	

		s.send(packet_1);
		while(true){
			try {
				s.receive(packet_2);
			} 
			catch (Exception e) {
				break;
			}	
			
			out.write(buffer_2, 0, buffer_2.length);
			s.send(ans);
		}	
		
		out.close();
	} 

	public void temp(String c) throws IOException{
		String temps, msg;	
		byte[] buffer_1= new byte[1024];
		byte[] buffer_2;	
		Vector <String> table=new Vector <String>();
		DatagramPacket packet_1=new DatagramPacket(buffer_1, 0, buffer_1.length);
		DatagramPacket packet_2;
		File xls=new File("temperatures.xls");
		FileWriter w=new FileWriter(xls);

		for (int i=1; i<=80; i++){
			msg=c+i;
			buffer_2= new byte[msg.length()];
			buffer_2=msg.getBytes();
			packet_2=new DatagramPacket(buffer_2, 0, buffer_2.length, serverAdress, server_port);
			s.send(packet_2);
			s.receive(packet_1);
			temps = new String(packet_1.getData(), 0, packet_1.getLength());
			table.add(temps);	
		}

		for(int i=0; i<table.size(); i++)
			w.write(table.get(i)+"\n");
		
		w.close();
	}

	public void sound(String c) throws LineUnavailableException, IOException{
		AudioFormat linearPCM = new AudioFormat(8000,8,1,true,false);
		SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
		lineOut.open(linearPCM,32000);
		String msg=c;
		int nib=0;
		int step=0;
		byte[] buffer_1=new byte[msg.length()];
		byte[] buffer_2=new byte[128];
		byte[] buffer_3=new byte[128];
		byte[] audio=new byte[255744];
		buffer_1=msg.getBytes();	
		DatagramPacket packet_1=new DatagramPacket(buffer_1, 0, buffer_1.length, serverAdress, server_port);
		DatagramPacket packet_2=new DatagramPacket(buffer_2, 0, buffer_2.length);
		File file = new File(c);
		FileWriter w=new FileWriter(file);

		s.send(packet_1);
		
		for(int i=0; i<999; i++){
			
			s.receive(packet_2);
			
			buffer_3= packet_2.getData();
			
			for(int j=0; j<128; j++){
				int x1=(((buffer_3[j])&0xf0)>>4)-8+nib;
				int x2=(buffer_3[j]&0x0f)-8;

				if(x1>127){
					x1=127;
				}
				if(x1<-128){
					x1=-128;
				}
				x2+=x1;
				if(x2>127){
					x2=127;
				}
				if(x2<-128){
					x2=-128;
				}
				nib=x2;
				audio[step]=(byte)x1;
				step++;
				audio[step]=(byte)x2;
				step++;
				
				try {
					w.write(x1+"\n"+x2+"\n");
				} 
				catch (IOException e) {
					e.printStackTrace();
				};
			} 
		}
		
		w.close();
		lineOut.start();
		lineOut.write(audio,0,audio.length);
		lineOut.stop();
		lineOut.close();	
	}



	public void sound_AQ(String c) throws LineUnavailableException, IOException{
	
		AudioFormat linearPCM = new AudioFormat(8000,16,1,true,false);
		SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
		lineOut.open(linearPCM,32000);
		String message=c;
		byte[] buffer_1= new byte[message.length()];
		byte[] buffer_2= new byte[132];
		byte[] buffer_3= new byte[132];
		byte[] audio= new byte[511488];
		byte sign;
		int nib=0;
		int m=0;
		int b=0;
		int x1=0;
		int x2=0;
		int step=0;
		File xls=new File(c+"times.xls");
		File xls2=new File(c+".xls");
		FileWriter w=new FileWriter(xls);
		FileWriter w2=new FileWriter(xls2);
		buffer_1=message.getBytes();	
		DatagramPacket p1=new DatagramPacket(buffer_1, 0, buffer_1.length, serverAdress, server_port);
		DatagramPacket r1=new DatagramPacket(buffer_2, 0, buffer_2.length);

		s.send(p1);
		
		for(int i=0; i<999; i++){
			s.receive(r1);
			buffer_3= r1.getData();
			byte[] head = new byte[4];
			
			if ((buffer_3[1]&0x80)!=0)
				sign=(byte)0xff;
			else
				sign=0;

			head[3]=sign;
			head[2]=sign;
			head[1]=buffer_3[1];
			head[0]=buffer_3[0];
			m=ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
			if ((buffer_3[3]&0x80)!=0)	
			sign=(byte)0xff;
			else
			sign=0;
			head[3]=sign;
			head[2]=sign;
			head[1]=buffer_3[3];
			head[0]=buffer_3[2];
			b=ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
			w.write(m+"	"+b+"\n");

			for(int j=4; j<132; j++){
				x1=((buffer_3[j])>>4)&0xf0;
				x2=buffer_3[j]&0xf;

				int d1=(x1-8)*b;
				int d2=(x2-8)*b;
				x1=d1+nib;
				x2=d2+d1;
				nib=d2; 
				x1+=m;
				x2+=m; 
				if(x1/256>127)
				x1=127;
				if(x1/256<-128)
				x1=-128;
				if(x2/256>127)
				x2=127;
				if(x2/256<-128)
				x2=-128;

				audio[step]=(byte)x1;
				step++;
				audio[step]=(byte)(x1/256);
				step++;
				audio[step]=(byte)x2;
				step++;
				audio[step]=(byte)(x2/256);
				step++;
				w2.write(x1+"\n"+x2+"\n");
			} 
		}
		
		w.close();
		w2.close();
		lineOut.start();
		lineOut.write(audio,0,audio.length);
		lineOut.stop();
		lineOut.close();
	}

	public static void main(String args[]) throws IOException, LineUnavailableException{
		application ap= new application();
		ap.serverAdress = InetAddress.getByName("155.207.18.208");
		ap.s=new DatagramSocket(ap.client_port);
		ap.s.setSoTimeout(9000);	
		//ap.echo(ap, ap.echo_code);
		//ap.echo(ap, "E0000");	
		//ap.image(ap, ap.picture_code+"CAM=1");
		//ap.image(ap, ap.picture_code+"CAM=2");
		//ap.temp(ap.echo_code+"T");
		ap.sound(ap.sound_code+"L03F999");
		ap.sound_AQ(ap.sound_code+"AQL03F999");
	}
}
