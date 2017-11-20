/*jshint esversion: 6 */
/*jshint asi: true */
const {AutoLanguageClient, DownloadFile} = require('atom-languageclient')
const ClientUtils = require('./clientutils')
const cp = require("child_process")

class ScalaLanguageClient extends AutoLanguageClient {
  getGrammarScopes () { return [ 'source.scala' ] }
  getLanguageName () { return 'Scala' }
  getServerName () { return 'Ensime' }

  constructor () {
    super()
    this.statusElement = document.createElement('span')
    this.statusElement.className = 'inline-block'
    this.ensimeFiles = []
  }

  filterChangeWatchedFiles (filePath) {
    for (var file in this.ensimeFiles) {
      if (file === filePath) {
        return true
      }
    }
    return false
  }

  startServerProcess (projectPath) {
    const logLevel = atom.config.get('ensime-scala.logLevel')
    const javaHome = atom.config.get('ensime-scala.javaHome')
    const extraArgs = atom.config.get('ensime-scala.virtualMachine.extraArgs')

    let clientProject = new ClientUtils.LspClientProject(
      projectPath,
      logLevel,
      javaHome === null ? undefined : javaHome,
      extraArgs === null ? undefined : extraArgs
    );

    return clientProject.checkRequirements().then(_ =>
      clientProject.javaArgs.then(javaArgs =>
      clientProject.ensimeFilePath.then(ensimeFilePath => {

        this.ensimeFiles.push(ensimeFilePath)

        const childProcess = cp.spawn(clientProject.javaCommand, javaArgs)
                this.captureServerErrors(childProcess)
        childProcess.on('exit', exitCode => {
          if (!childProcess.killed) {
            atom.notifications.addError('IDE-Scala language server stopped unexpectedly.', {
              dismissable: true,
              description: this.processStdErr ? `<code>${this.processStdErr}</code>` : `Exit code ${exitCode}`
            })
          }
          this.updateStatusBar('Stopped')
        })
        return childProcess
      })))
  }

  preInitialization (connection) {
    connection.onCustom('language/status', (e) => this.updateStatusBar(`${e.type.replace(/^Started$/, '')} ${e.message}`))
    connection.onCustom('language/actionableNotification', this.actionableNotification.bind(this))
  }

  updateStatusBar (text) {
    this.statusElement.textContent = `${this.name} ${text}`
  }

  actionableNotification (notification) {
    const options = { dismissable: true, detail: this.getServerName() }
    if (Array.isArray(notification.commands)) {
      options.buttons = notification.commands.map(c => ({ text: c.title, onDidClick: (e) => onActionableButton(e, c.command) }))
      // TODO: Deal with the actions
    }

    const notificationDialog = this.createNotification(notification.severity, notification.message, options)

    const onActionableButton = (event, commandName) => {
      const commandFunction = this.commands[commandName]
      if (commandFunction != null) {
        commandFunction()
      } else {
        console.log(`Unknown actionableNotification command '${commandName}'`)
      }
      notificationDialog.dismiss()
    }
  }

  createNotification (severity, message, options) {
    switch (severity) {
      case 1: return atom.notifications.addError(message, options)
      case 2: return atom.notifications.addWarning(message, options)
      case 3: return atom.notifications.addInfo(message, options)
      case 4: console.log(message)
    }
  }

  consumeStatusBar (statusBar) {
    this.statusTile = statusBar.addRightTile({ item: this.statusElement, priority: 1000 })
  }
}

module.exports = new ScalaLanguageClient()
