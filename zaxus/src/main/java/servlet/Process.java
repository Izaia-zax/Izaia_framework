package main.java.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class Process extends HttpServlet{
    public void processRequest(HttpServletRequest req , HttpServletResponse res) throws ServletException, IOException{
        res.setContentType("text/plain");
        String url = req.getRequestURL().toString();
        PrintWriter out = res.getWriter();
        out.println(url);
    }

    @Override
    public void doGet(HttpServletRequest req , HttpServletResponse res) throws ServletException, IOException{
        processRequest(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req , HttpServletResponse res) throws ServletException, IOException{
        processRequest(req, res);
    }
}