package org.example.controller;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServlet;

import java.util.logging.Logger;

public abstract class Controller extends HttpServlet {
    Logger logger;

    abstract void initLogger();

    final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
}
