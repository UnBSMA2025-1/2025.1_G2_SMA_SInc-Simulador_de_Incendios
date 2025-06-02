import jade.wrapper.ContainerController;

public class ImageMapConverter {

    public static EnhancedMap createMapFromImage(String imagePath, String pythonScriptPath,
                                                 int width, int height,
                                                 ContainerController container) {
        EnhancedMap map = new EnhancedMap(width, height, container);

        if (map.convertImageToMap(imagePath, pythonScriptPath, width, height, "color")) {
            System.out.println("Map created successfully from image!");
            System.out.println(map.generateImageMapReport());
            return map;
        } else {
            System.err.println("Failed to create map from image. Using random generation.");
            return map; // Falls back to random generation from parent constructor
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ImageMapConverter <image_path> <python_script_path>");
            System.out.println("Example: java ImageMapConverter forest.jpg image_to_map_converter.py");
            return;
        }

        String imagePath = args[0];
        String pythonScriptPath = args[1];

        ContainerController container = null; // Initialize with your container

        EnhancedMap map = createMapFromImage(imagePath, pythonScriptPath, 50, 50, container);

        System.out.println(map.generateImageMapReport());
    }
}