package wang.relish.widget.delv

/**
 * @author relish
 * @since 20191019
 */
interface IGroup<Child> {

    fun children(): List<Child>
}
