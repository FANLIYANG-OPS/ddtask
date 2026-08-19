# DDTask 构建入口
# 用法: make / make pack / make clean

JAVA_HOME ?= /usr/lib/jvm/java-21-openjdk-amd64
export JAVA_HOME

# 可通过环境变量覆盖，例如: GRADLE=./gradlew make pack
GRADLE ?= $(shell \
	if ./gradlew --version >/dev/null 2>&1; then echo ./gradlew; \
	elif [ -x /tmp/gradle-8.2/bin/gradle ]; then echo /tmp/gradle-8.2/bin/gradle; \
	else echo gradle; fi)
APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
APK_OUT := ddtask-debug.apk
APK_WIN := /mnt/c/Users/17799/Desktop/ddtask-debug.apk

.PHONY: help debug pack release clean

help:
	@echo "DDTask Makefile"
	@echo ""
	@echo "  make debug    编译 debug APK"
	@echo "  make pack     编译并复制到 $(APK_OUT)"
	@echo "  make release  编译 release APK"
	@echo "  make clean    清理构建产物"
	@echo ""
	@echo "环境变量: JAVA_HOME (当前: $(JAVA_HOME))"
	@echo "          GRADLE   (当前: $(GRADLE))"

debug:
	$(GRADLE) assembleDebug

pack: debug
	cp $(APK_DEBUG) $(APK_OUT)
	@ls -lh $(APK_OUT)
	@echo "version: $$(grep versionName app/build.gradle.kts | head -1 | sed 's/.*= \"//;s/\"//')"

win: debug
	cp $(APK_DEBUG) $(APK_WIN)
	@ls -lh $(APK_WIN)
	@echo "version: $$(grep versionName app/build.gradle.kts | head -1 | sed 's/.*= \"//;s/\"//')"
	
release:
	$(GRADLE) assembleRelease

clean:
	$(GRADLE) clean
