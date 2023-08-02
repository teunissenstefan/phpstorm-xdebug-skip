package de.tschallacka.phpstormxdebugskip.listeners

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Trinity
import com.intellij.xdebugger.XDebugSessionListener
import de.tschallacka.phpstormxdebugskip.settings.Settings // Assuming this is the location of your Settings class

class MyXDebugProcessListener(private val project: Project) : XDebuggerManagerListener {
    override fun processStarted(debugProcess: XDebugProcess) {
        val session = debugProcess.session
        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                val frame = session.currentStackFrame
                val frameDataObject = frame?.getEqualityObject()
                if (frameDataObject is Trinity<*, *, *>) {
                    val frameData = frameDataObject as Trinity<Int, String, String>
                    val path = frameData.second
                    val functionName = frameData.third
                    val regex = Regex("(.*)->.*")
                    val match = regex.matchEntire(functionName)
                    val namespace = match?.groupValues?.get(1)
                    if (namespace != null && isSkippableNamespace("\\"+namespace)) {
                        session.stepInto()
                        return
                    }
                    if (path != null && isSkippableFilePath(path)) {
                        session.stepInto()
                        return
                    }
                }
            }
        })
    }

    private fun isSkippableNamespace(namespace: String): Boolean {
        val settings = Settings.getInstance()
        val namespaces = settings.settingsState.namespaces;
        val match = namespaces.any {
            ns -> namespace.contains(ns)
        }
        return match
    }

    private fun isSkippableFilePath(path: String): Boolean {
        val settings = Settings.getInstance()
        val filepaths = settings.settingsState.filepaths;
        val match = filepaths.any {
            filepath -> path.contains(filepath)
        }
        return match
    }
}