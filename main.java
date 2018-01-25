import java.io.*;

import javax.comm.*;


public class SerialCommApp {
	CommPortIdentifier portId;
	SerialPort asyncPort;
	InputStream ip;
	OutputStream op;
	String txMessage, rxMessage;
	long []times;
	
	public SerialCommApp(){
		txMessage=null;
		rxMessage=null;
		times=new long[500];
		
		try {
				portId = CommPortIdentifier.getPortIdentifier("COM3");
		} catch (NoSuchPortException e) {
				e.printStackTrace();
		}
		try {
			asyncPort= (SerialPort)portId.open("serial port 3", 4000);
		} catch (PortInUseException e) {
			e.printStackTrace();
		}
		try {
			asyncPort.setSerialPortParams(28800, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			asyncPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN & SerialPort.FLOWCONTROL_RTSCTS_OUT);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
		asyncPort.setDTR(true);
		//Request To Send = true etsi wste na borw na stelnw dedomena
		asyncPort.setRTS(true);
		
		try {
			op=asyncPort.getOutputStream();
			ip=asyncPort.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public int monitorDialProcess() {
		//  NOTE : x3 is needed to ignore NO DIALTONE message.
		//  NOTE : \r is the Java escape code for the Carriage Return Key. 
		int k;
		
		txMessage="atx3dt2310994186\r";
		
		rxMessage="";   
		
	   	try {
			op.write(txMessage.getBytes());
			
			for (;;) {
			    if (ip.available()>0) {
					k=ip.read();
					rxMessage+=(char)k;
					System.out.print((char)k);
			    }
				if ((rxMessage.indexOf("\r\n\n\n")>-1)){
					rxMessage="";
					break;
				}
				if ((rxMessage.indexOf("\nNO CARRIER")>-1)||(rxMessage.indexOf("BUSY")>-1)){
					//an vrethei eite i leksi no carrier eite i leksi busy sto input i efarmogi termatizei
					return 0;
				}
			}	
	  	} catch (IOException e) {
			e.printStackTrace();
		}		 	
		
	   	//perimene gia miso deuterolepto
	   	try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	   	
	   	return 1;
	}
	
	
	long echoRequest(String code){
		int k;
		String message="echo_request_code "+code+"\r";
		long time=0;
		time=System.currentTimeMillis();
				
		try {
			op.write(message.getBytes());
			
			
			for (;;) {
			    if (ip.available()>0) {
					k=ip.read();
					rxMessage+=(char)k;
					System.out.print((char)k);
			    }
			    if ((rxMessage.indexOf("PSTOP")>-1)){
			    	rxMessage="";
					break;
				}
			    if ((rxMessage.indexOf("\nNO CARRIER")>-1)){
			    	//an vrethei i leksi no carrier sto input stream termatizoume tin efarmogi
					return 0;
				}
			}		
	  	} catch (IOException e) {
			e.printStackTrace();
		}		 	
		
		time=System.currentTimeMillis()-time;
		
		return time;
	}
	
	
	long imageRequest(String code){
		int k;
		String message="image_request_code "+code+"\r";
		long time=0;
		long limit=2*60*1000; //limit= 2 lepta
		time=System.currentTimeMillis();
		
		String filePath = code+".jpg";
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
				
		try {
			op.write(message.getBytes());
			
			
			for (;;) {
			    if (ip.available()>0) {
					k=ip.read();
					fos.write((byte)k);
					rxMessage+=(char)k;
					System.out.print((char)k);
			    }
			    if ((System.currentTimeMillis()-time)>limit){
			    	//an kseperasei ta 2 lepta simainei oti i lipsi oloklirwthike
			    	fos.close();
			    	rxMessage="";
					break;
				}
			    if ((rxMessage.indexOf("\nNO CARRIER")>-1)){
			    	//an vrethei i leksi no carrier sto input stream termatizoume tin efarmogi
					return 0;
				}
			}		
	  	} catch (IOException e) {
			e.printStackTrace();
		}		 	
		
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		time=System.currentTimeMillis()-time;
		
		return time;
	}
	
	
	long burstRequest(String Tcode, String Bcode, String Pcode, String Dcode){
		int k;
		String message="burst_request_code "+Tcode+Bcode+Pcode+Dcode+"\r";
		
		String packets=Pcode.substring(1);
		int nPackets= Integer.parseInt(packets);
		//countP : o metritis paketwn
		int countP=0;
		
		String bursts=Bcode.substring(1);
		int nBursts= Integer.parseInt(bursts);
		//countB : o metritis twn bursts
		int countB=0;
		
		//to arxeio sto opoio tha apothikevodai ta dedomena
		File file = new File("dataBurst.xls");
	    FileWriter writer = null;
	    try {
			writer = new FileWriter(file);
		} catch (IOException e) {
			e.printStackTrace();
		}				
	
		long time=0,time2=0;
		long limit=1*60*1000; //limit= 1 lepto
		time=System.currentTimeMillis();
		time2=System.currentTimeMillis();
		
		try {
			op.write(message.getBytes());
			
			for (;;) {
			    if (ip.available()>0) {
			    	k=ip.read();
					rxMessage+=(char)k;
					System.out.print((char)k);
			    }
			    if (rxMessage.indexOf("PSTOP")>-1){
			    	rxMessage="";
			    	countP++;
				}
			    
			    if (countP==nPackets){
			    	try {
					    writer.write(System.currentTimeMillis()-time2+"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}; 
					time2=System.currentTimeMillis();
			    	countP=0;
			    	countB++;
			    }
			    
			    if (countB==nBursts){
			    	rxMessage="";
			    	break;
			    }
			    
			    if (rxMessage.indexOf("NO CARRIER")>-1){
			    	//no carrier
			    	if (writer != null)
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					return 0;
				}
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}	
			
		time=System.currentTimeMillis()-time;
		
		if (writer != null)
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		return time;
	}
	
	
	long automaticRepeatRequest(String Qcode, String Rcode){
		int k;
		int ack=0;
		int nack=0;
		
		//to arxeio sto opoio tha apothikevodai ta dedomena
				File file = new File("dataARQ.xls");
			    FileWriter writer = null;
			    try {
					writer = new FileWriter(file);
				} catch (IOException e) {
					e.printStackTrace();
				}	
		
		String message=Qcode+"\r";
		long time=0;
		long time2=0;
		long limit=4*60*1000;//limit =4 lepta
		time=System.currentTimeMillis();
		
		while(System.currentTimeMillis()-time<limit){
			try {
				op.write(message.getBytes());
				time2=System.currentTimeMillis();
				for (;;) {
				    if (ip.available()>0) {
						k=ip.read();
						rxMessage+=(char)k;
						System.out.print((char)k);
				    }
				    if ((rxMessage.indexOf("PSTOP")>-1)){
				    	String FCS=rxMessage.substring(49,52);
				    	String MSG=rxMessage.substring(31,47);
				    	char xor=MSG.charAt(0);
			    	
				    	for (int i=1; i<MSG.length(); i++){
				    		xor=(char)(MSG.charAt(i)^xor);
				    	}
				    	
				      	int fcs=Integer.parseInt(FCS);
				    	if(fcs==(int)xor){
				    		//ACK
				    		message=Qcode+"\r";
				    		ack++;
				    		time2=System.currentTimeMillis()-time2;
				    		try {
							    writer.write(time2+"  "+nack+"  "+ack+"\n");
							    ack=0;
							    nack=0;
							} catch (IOException e) {
								e.printStackTrace();
							}; 
				    	}
				    	else{
				    		//NACK
				    		message=Rcode+"\r";
				    		nack++;
				    	}
				    	rxMessage="";
				    	break;
					}
				    if ((rxMessage.indexOf("\nNO CARRIER")>-1)){
				    	//an vrethei i leksi no carrier sto input stream termatizoume tin efarmogi
						return 0;
					}
				}		
		  	} catch (IOException e) {
				e.printStackTrace();
			}		 	
		}
		
		time=System.currentTimeMillis()-time;
		
		System.out.println("\nACK: "+ack+", NACK: "+nack);
		
		if (writer != null)
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		
		return time;
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SerialCommApp com=new SerialCommApp();
		//Echo request code
		String Ecode="E8298";
		
		//Image request code, error free
		String Mcode="M1745";
		
		//Image request code, with errors
		String Gcode="G6554";
		
		//Burst request code
		String Tcode="T6788";
		String Bcode="B90";
		String Pcode="P90";
		String Dcode="D200";
		
		//ACK result code
		String Qcode="Q6404";
		
		//NACK result code
		String Rcode="R9298";
		
		long result=1;
		long time=0;
		
					
		if (com.monitorDialProcess()==0){
			//No carrier
			System.out.println("\nprogramme was stopped");
		}
		else{
			/////////////////////ECHO PACKETS///////////////////////////
			//to arxeio sto opoio tha apothikevodai ta dedomena
			File file = new File("dataEcho.xls");
		    FileWriter writer = null;
		    try {
				writer = new FileWriter(file);
			} catch (IOException e) {
				e.printStackTrace();
			}		
			
			long limit=4*60*1000; //limit= 4 lepta
			time=System.currentTimeMillis();
						
			//lipsi paketwn echoPacket gia 4 lepta
			while ((System.currentTimeMillis()-time)<limit){
				result=com.echoRequest(Ecode);
				//an to apotelesma einai 0 simainei No carrier
				if (result==0) break;
				
				//o xronos eggrafis se arxeio einai polu mikros (0 ms) opote den epireazei ti metrisi
				try {
				    writer.write(result+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				};
			}
			
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			System.out.println("\nTelos lipsis twn echo packets!");
			
			/////////////////////end of ECHO PACKETS////////////////////
			
			//perimene gia miso deuterolepto
		   	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		   	////////////////////IMAGE, error free///////////////////////
			if (result!=0){
				result=com.imageRequest(Mcode);
				System.out.println("\nTelos lipsis tis eikonas (error free)!");
				
			}
			//////////////////end of IMAGE, error free//////////////////
			
			//perimene gia miso deuterolepto
		   	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			
			////////////////////IMAGE, with errors//////////////////////
			if (result!=0){
				result=com.imageRequest(Gcode);
				System.out.println("\nTelos lipsis tis eikonas (with errors)!");
			}
			/////////////////end of IMAGE, with errors//////////////////
			
			//perimene gia miso deuterolepto
		   	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		   	
		   	
		   	/////////////////////////////Burst//////////////////////////
		   	if (result!=0){
				result=com.burstRequest(Tcode,Bcode,Pcode,Dcode);
				System.out.println("\nTelos lipsis tis akolouthias apo burst!");
			}
		   	/////////////////////////end of Burst///////////////////////
		   	
		    //perimene gia miso deuterolepto
		   	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		   	
			
			//////////////////////////////ARQ///////////////////////////
		   	if (result!=0){
		   		result=com.automaticRepeatRequest(Qcode, Rcode);
		   		System.out.println("\nTelos tis diadikasias ARQ!");
		   	} 
		   	//////////////////////////end of ARQ////////////////////////
		}
	        

		System.out.println("\nbb!!");
	}

}
