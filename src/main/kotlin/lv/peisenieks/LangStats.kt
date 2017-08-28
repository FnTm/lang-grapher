package lv.peisenieks

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.filter.AndRevFilter
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat


fun getRepo(repoPath: String, branch: String, numDays: Long, cutOffDate: LocalDateTime): String? {
    val repoDir = File(repoPath + "/.git")
    if (repoDir.exists()) {
        FileRepositoryBuilder().setGitDir(repoDir)
                .readEnvironment()
                .addCeilingDirectory(File(repoPath))
                .findGitDir()
                .build()?.let { repository ->
            val repo = Git(repository)

            val head = repository.exactRef("refs/heads/$branch")
            if (head == null) {
                println("Error! The provided branch doesn't exist ")
                return null
            }
            System.out.println("Having repository: " + repository.directory)

            val stats = getCommitsForDay(repoPath, repo, LocalDateTime.now().withHour(0).withMinute(0), numDays, cutOffDate)
            val jsonStats = Gson().toJson(stats)
            println(jsonStats)

            return jsonStats
        }
    } else {
        println("Error! Can't find a Git repo in the provided directory")
    }
    return null
}

fun getStats(repoPath: String, commitHash: String): Map<String, String>? {
    val stats = "git-linguist --commit=$commitHash stats".runCommand(File(repoPath))
    return splitStats(stats)
}

private fun getCommitsForDay(repoPath: String, repo: Git, date: LocalDateTime, lookbackDays: Long, cutOffDate: LocalDateTime): MutableMap<LocalDateTime, Map<String, String>?> {
    val nret = mutableMapOf<LocalDateTime, Map<String, String>?>()
    for (i in 0..lookbackDays) {
        val nDay = LocalDateTime.from(date).minusDays(i)

        if (nDay.isBefore(cutOffDate)) continue

        println("Stats for $nDay, $i/$lookbackDays")
        val startDate = LocalDateTime.from(nDay).withHour(0).withMinute(0)
        val endDate = LocalDateTime.from(nDay).withHour(23).withMinute(59)
        val filter = AndRevFilter.create(
                CommitTimeRevFilter.after(Date.from(startDate.toInstant(ZoneOffset.UTC))),
                CommitTimeRevFilter.before(Date.from(endDate.toInstant(ZoneOffset.UTC))))
        val method = repo.log()
                .setRevFilter(filter)

        val revs = method.call()

        revs?.firstOrNull()?.let {
            println("Commit ${it.name}")
            nret.put(nDay, getStats(repoPath, it.name))
            1L
        } ?: println("No commits for $nDay")
    }
    return nret
}

fun splitStats(runCommand: String?): Map<String, String>? {
    var nmap = mutableMapOf<String, String>()
    runCommand?.let { it ->

        nmap = Gson().fromJson(it)
    }
    return nmap
}

fun main(args: Array<String>) {
    if (args.size < 3) {
        throw IllegalArgumentException("The script expects arguments in the form of \"AbsDirPath nameOfGitBranch numOfDaysToLookBack [cutOffEpoch]\"")
    }
    val rnbase = args[0]
    val branch = args[1]
    val lookbackDays = args[2].toLong()

    val cutOffDate = if (args.size == 4) {
        args[3].toLong()
    } else {
        0
    }

    val langJson: String = getRepo(rnbase, branch, lookbackDays, LocalDateTime.ofEpochSecond(cutOffDate, 0, ZoneOffset.UTC)) ?: return
    val branchName = branch.replace("-", "").replace("/", "")

    val project = rnbase.split("/").last()

    val dateNow = SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().time)

    File("output").mkdirs()

    val fileNameBase = "$project-$branchName-$dateNow-${lookbackDays}days"
    File("output/$fileNameBase.json").writeText(langJson)

    DataGrapher(File("output/$fileNameBase.jpg"), langJson)
}

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(1, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}