// SPDX-License-Identifier: GPL-3.0-only
// launch only the bundled java runtime, with paths relative to this executable
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#define CAPACITY 32768
static WCHAR folder[CAPACITY];
static WCHAR java_path[CAPACITY];
static WCHAR jar_path[CAPACITY];
static WCHAR command[CAPACITY];

static BOOL append(WCHAR *target, const WCHAR *source) {
    DWORD i = 0;
    while (target[i] && i < CAPACITY - 1) i++;
    while (*source) {
        if (i >= CAPACITY - 1) return FALSE;
        target[i++] = *source++;
    }
    target[i] = 0;
    return TRUE;
}

static void failure(void) {
    MessageBoxW(NULL,
        L"Could not start LastStandinNote. Extract the entire ZIP and keep all folders together.\n\n"
        L"No se pudo iniciar LastStandinNote. Extrae el ZIP completo y conserva todas las carpetas juntas.",
        L"LastStandinNote", MB_OK | MB_ICONERROR);
    ExitProcess(1);
}

void WINAPI launcher_entry(void) {
    DWORD length = GetModuleFileNameW(NULL, folder, CAPACITY);
    if (!length || length >= CAPACITY) failure();
    while (length && folder[length - 1] != L'\\') length--;
    if (!length) failure();
    folder[length - 1] = 0;

    if (!append(java_path, folder) || !append(java_path, L"\\runtime\\bin\\javaw.exe")
            || !append(jar_path, folder) || !append(jar_path, L"\\app\\LastStandinNote.jar")) failure();
    if (GetFileAttributesW(java_path) == INVALID_FILE_ATTRIBUTES
            || GetFileAttributesW(jar_path) == INVALID_FILE_ATTRIBUTES) failure();
    if (!append(command, L"\"") || !append(command, java_path)
            || !append(command, L"\" -Xmx512m -Dfile.encoding=UTF-8 -jar \"")
            || !append(command, jar_path) || !append(command, L"\"")) failure();

    STARTUPINFOW startup;
    PROCESS_INFORMATION process;
    unsigned char *bytes = (unsigned char *) &startup;
    for (DWORD i = 0; i < sizeof(startup); i++) bytes[i] = 0;
    startup.cb = sizeof(startup);
    if (!CreateProcessW(java_path, command, NULL, NULL, FALSE, 0, NULL,
            folder, &startup, &process)) failure();
    CloseHandle(process.hThread);
    WaitForSingleObject(process.hProcess, INFINITE);
    DWORD code = 1;
    GetExitCodeProcess(process.hProcess, &code);
    CloseHandle(process.hProcess);
    ExitProcess(code);
}
