import jade.wrapper.ContainerController;

/**
 * Utility class for image-to-map conversion
 */
public class ImageMapConverter {

    /**
     * Create an enhanced map from an image file
     */
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

    /**
     * Demo method showing how to use the image conversion
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ImageMapConverter <image_path> <python_script_path>");
            System.out.println("Example: java ImageMapConverter forest.jpg image_to_map_converter.py");
            return;
        }

        String imagePath = args[0];
        String pythonScriptPath = args[1];

        // Create container controller (you'll need to adapt this to your JADE setup)
        ContainerController container = null; // Initialize with your container

        // Create map from image
        EnhancedMap map = createMapFromImage(imagePath, pythonScriptPath, 50, 50, container);

        // Display report
        System.out.println(map.generateImageMapReport());
    }
}