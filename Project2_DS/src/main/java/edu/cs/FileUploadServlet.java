package edu.cs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.FileInputStream;
//import decodeCCDA.DecodeCCDA;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/CS370S25_soman_davin/FileUploadServlet")
@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB
        maxFileSize=1024*1024*50,      	// 50 MB
        maxRequestSize=1024*1024*100)   	// 100 MB
public class FileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 205242440643911308L;

    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "uploads";
    private static final String dbHost = "nimble-alpha-456322-s9:us-east1:mysql-instance";
    private static final String dbName = "cs370assn";
    private static final String dbUser = "client";
    private static final String dbPass = "cs370bonsy";
    private static final String dbSSL = "sslMode=REQUIRED";
    //private static final String dbUrl = "jdbc:mysql://" + dbHost + ":3306/" + dbName + "?" + "user=" + dbUser + "&password=" + dbPass + "&" + dbSSL;
    private static final String dbUrl = String.format("jdbc:mysql:///%s?cloudSqlInstance=%s&" + "socketFactory=com.google.cloud.sql.mysql.SocketFactory&user=%s&password=%s&%s",
    dbName, dbHost, dbUser, dbPass, dbSSL);

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // gets absolute path of the web application
        String applicationPath = request.getServletContext().getRealPath("");

        // constructs path of the directory to save uploaded file
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;

        // creates the save directory if it does not exists
        File fileSaveDir = new File(uploadFilePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdirs();
        }
        System.out.println("Upload File Directory="+fileSaveDir.getAbsolutePath());

        String fileName = "";
        File uploadedFile = null;
        //Get all the parts from request and write it to the file on server
        for (Part part : request.getParts()) {
            fileName = getFileName(part);
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            String filePath = uploadFilePath + File.separator + fileName;
            part.write(filePath);
            uploadedFile = new File(filePath);
        }

        String message = "Result";
        String content = null;
        if (uploadedFile != null) {
            content = new Scanner(uploadedFile).useDelimiter("\\Z").next();
        } else {
            content = "No file uploaded";
        }
        //response.getWriter().write(message + "<BR>" + content);
        writeToResponse(response, message + "<BR>" + content);

        PreparedStatement preparedStatement = null;
        Connection connect = null;
        FileInputStream fileDataStream = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connect = DriverManager.getConnection(dbUrl);

            System.out.println("SSL connection to MySQL database : Success");
            System.out.println("Attempting to insert file into database");
            // Decode the CCDA file
            //DecodeCCDA parsed = new DecodeCCDA(uploadFilePath + File.separator + fileName);
            //writeToResponse(response, parsed.getjson());

            //Inserting the file into the uploaded file table
            String insertFile = "INSERT INTO UploadedFiles (file_name, file_data) VALUES (? , ?)";
            fileDataStream = new FileInputStream(uploadedFile);
            preparedStatement = connect.prepareStatement(insertFile);
            preparedStatement.setString(1, fileName);
            preparedStatement.setBlob(2, fileDataStream, (int)uploadedFile.length());

            int rowsInsert = preparedStatement.executeUpdate();
            if (rowsInsert > 0) {
                System.out.println("File uploaded and saved into database");
            } else {
                System.out.println("File upload failed");
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database driver not found");
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (IOException e) {
            System.out.println("File handling error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "File handling error");
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
        }
        finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (connect != null) {
                try {
                    connect.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

    }

    /**
     * Utility method to get file name from HTTP header content-disposition
     */
    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= "+contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length()-1);
            }
        }
        return "";
    }


    private void writeToResponse(HttpServletResponse resp, String results) throws IOException {
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        resp.setContentType("text/plain");

        if (results.isEmpty()) {
            writer.write("No results found.");
        } else {
            writer.write(results);
        }
        writer.close();
    }


}
