package com.rehan.monolith;

interface IShizukuShellService {
    void destroy() = 16777114;
    String exec(String command) = 1;
    int getUid() = 2;
}
