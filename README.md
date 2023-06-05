# shelfDetector

Приложение определяет bounding boxes продуктов на полках, используя tensorFlow lite.

Класс SDetector является моделью. Он работает на основе Interpreter из tensorflow.lite. MainViewModel получает данные и обрабатывает их, два фрагмента: PhotoFragment и CameraFragment слушают изменения.

DetectionsView рисует bounding boxes. Красным выделяются товары, зеленым - полки.

### Галерея:

![Screenshot_20230605-161007_Shelf detector](https://github.com/dkkdark/shelfDetector/assets/49618961/01a8c32d-5bc8-42b4-885d-4ce90957139c)


![Screenshot_20230605-161025_Shelf detector](https://github.com/dkkdark/shelfDetector/assets/49618961/035967ed-c3c3-4713-b9ff-45e90cb06c0a)


![Screenshot_20230605-161045_Shelf detector](https://github.com/dkkdark/shelfDetector/assets/49618961/ef6eeb44-5362-4ccb-b2fc-1ae04f7dbdcd)


### Камера и зум (gif):

![YouCut_20230605_174218959-2-min](https://github.com/dkkdark/shelfDetector/assets/49618961/835f3ec8-1189-4ad1-b465-490a18218bd8)
