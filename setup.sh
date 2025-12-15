#!/bin/bash

echo "🚀 KMP Template Setup"
echo "===================="
echo ""

# 检查 JDK
echo "📝 Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "✅ Java found: version $JAVA_VERSION"
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "⚠️  Warning: Java 17+ recommended, you have $JAVA_VERSION"
    fi
else
    echo "❌ Java not found. Please install JDK 21+"
    exit 1
fi

echo ""

# 创建 local.properties
if [ ! -f "local.properties" ]; then
    echo "📝 Setting up local.properties..."
    
    # 尝试自动检测 Android SDK
    if [ -d "$HOME/Library/Android/sdk" ]; then
        SDK_PATH="$HOME/Library/Android/sdk"
        echo "sdk.dir=$SDK_PATH" > local.properties
        echo "✅ Android SDK found at: $SDK_PATH"
    elif [ -d "$ANDROID_HOME" ]; then
        echo "sdk.dir=$ANDROID_HOME" > local.properties
        echo "✅ Android SDK found at: $ANDROID_HOME"
    else
        echo "⚠️  Android SDK not found automatically."
        echo "Please create local.properties manually with your SDK path."
        cp local.properties.template local.properties
    fi
else
    echo "✅ local.properties already exists"
fi

echo ""
echo "🔧 Making gradlew executable..."
chmod +x gradlew

echo ""
echo "✅ Setup complete!"
echo ""
echo "📱 Next steps:"
echo "  • Android:  ./gradlew :app:android:installDebug"
echo "  • Desktop:  ./gradlew :app:desktop:run"
echo "  • Clean:    ./gradlew clean"
echo ""
echo "Happy coding! 🎉"
