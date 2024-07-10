# Disclaimer

This is a hobby project. I work on it for fun.
If I'm not having fun, I probably won't do it :)
You're welcome to report issues, or create pull requests.
But if they're not useful to me, I probably won't respond
(though they may help others who run into them).
If you really want to see enhancements, fork it and have fun. :)

# Development environment

Any Java IDE of your choice. However the main window form was creating using Netbeans form designer, so you'll want that to edit the GUI.

# Building

It's a standard Maven project. Run
```
mvn clean install
```
It will generate a self-contained fat jar in the `/target/` directory that is ready to use.

# Reporting issues

Use issue reporting best practices. Provide these 3 things:

1. Steps to reproduce the issue
2. Expected behavior
3. Actual behavior

# Pull requests

Check your code against SpotBugs, PMD, or IntelliJ inspections. Fix any issues.

Anything that is merged will be a fast-forward or squash-merge.
