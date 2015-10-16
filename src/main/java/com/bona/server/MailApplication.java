package com.bona.server;

import com.bona.server.pop3.POP3Server;
import com.bona.server.pop3.api.Handler.DefaultAuthHandler;
import com.bona.server.pop3.core.factory.FileStorageFactory;
import org.mailster.smtp.AllSchemesAuthenticationHandler;
import org.mailster.smtp.SMTPServer;
import org.mailster.smtp.api.MessageListenerAdapter;
import org.mailster.smtp.api.handler.SessionContext;
import org.mailster.smtp.core.auth.LoginFailedException;
import org.mailster.smtp.core.auth.LoginValidator;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by bona on 2015/10/16.
 */
public class MailApplication {
    //smtp server
    private SMTPServer smtpServer;
    //pop3 server
    private POP3Server pop3Server;
    private File root;

    public MailApplication(){
        initApplication();
    }

    private void initApplication() {
        smtpServer = new SMTPServer(new MessageListenerAdapter(){
                @Override
                public boolean accept(SessionContext ctx, String from, String recipient) {
                    return true;
                }

                @Override
                public void deliver(SessionContext sessionContext, String from, String to , InputStream data) throws IOException {
                    System.out.println("inputstream class:"+data.getClass().getName());
                    System.out.println("smtp server session context credential id:"+sessionContext.getCredential().getId());
                    saveMail(to, data);
                }
        });
        pop3Server = new POP3Server();

    }

    protected void initServer(){
        smtpServer.setPort(25);
		smtpServer.setAuthenticationHandlerFactory(new AllSchemesAuthenticationHandler(new LoginValidator() {
			public void login(String username, String password)
					throws LoginFailedException {
				System.out.println("username="+username);
				System.out.println("password="+password);
			}
		}));



        pop3Server.setStorageFactory(new FileStorageFactory(getRoot()));
        pop3Server.setAuthenticationHandler(new DefaultAuthHandler(){
            @Override
            public boolean authUser(String userName, String password) {
                return super.authUser(userName, password);
            }
        });
        pop3Server.setPort(110);
    }

    public void start(){

        smtpServer.start();
        pop3Server.start();
    }

    static public void main(String[] argv) {

        MailApplication app = new MailApplication();
        app.setRoot(new File(".", "mails"));
        app.initServer();
        app.start();
    }

    public void setRoot(File root) {
//        System.setProperty(Constants.INBOX_STORAGE_DIR,root.getAbsolutePath());
        this.root = root;
    }

    protected File getUserDir(String userName){
        File ret= new File(root,userName);
        if(!ret.exists()) ret.mkdirs();
        return ret;
    }

    public File getRoot() {
        return root;
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-","");
    }

    public void saveMail(String userName,InputStream is){
        MimeMessage message = null;
        try {
            message = new MimeMessage(Session.getDefaultInstance(new Properties()),is);
            OutputStream os = new FileOutputStream(new File(getUserDir(userName),getUUID()+".eml"));
            message.writeTo(os);
            os.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
